package org.darren.stock.domain.stockSystem

import arrow.resilience.retry
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.actor4k.actor.ref.ActorRef
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.ProductQuantity
import org.darren.stock.domain.RetriableException
import org.darren.stock.domain.actors.StockPotProtocol
import org.darren.stock.domain.actors.StockPotProtocol.RecordDelivery
import org.darren.stock.util.LoggingHelper.wrapMethod
import java.time.LocalDateTime

data class DeliveryRequest(
    val locationId: String,
    val supplierId: String,
    val supplierRef: String,
    val deliveryDate: LocalDateTime,
    val products: List<ProductQuantity>,
    val requestId: String,
)

class IdempotencyContentMismatchException(
    message: String,
    val requestId: String,
) : Exception(message)

object Delivery {
    private val logger = KotlinLogging.logger {}

    suspend fun StockSystem.recordDelivery(deliveryRequest: DeliveryRequest) =
        coroutineScope {
            validateRequest(deliveryRequest, locations)

            val deliveryOperations =
                deliveryRequest.products.mapIndexed { index, product ->
                    async {
                        val stockPot = getStockPot(deliveryRequest.locationId, product.productId)

                        val recordDelivery =
                            buildRecordDelivery(deliveryRequest, product, index)

                        retryPolicy.retry<RetriableException, StockPotProtocol.Reply> {
                            processDeliveryAndReturnResult(stockPot, recordDelivery)
                        }
                    }
                }
            deliveryOperations.awaitAll()
        }

    private fun buildRecordDelivery(
        deliveryRequest: DeliveryRequest,
        product: ProductQuantity,
        index: Int,
    ): RecordDelivery {
        val recordDelivery =
            with(deliveryRequest) {
                RecordDelivery(
                    product.quantity,
                    supplierId,
                    supplierRef,
                    deliveryDate,
                    "$requestId-$index",
                )
            }
        return recordDelivery
    }

    suspend fun validateRequest(
        deliveryRequest: DeliveryRequest,
        locations: LocationApiClient,
    ) {
        locations.ensureLocationsAreTracked(deliveryRequest.locationId)

        deliveryRequest.products.forEach { product ->
            require(product.quantity > 0) { "Delivery quantity must be positive" }
        }
    }

    private suspend fun processDeliveryAndReturnResult(
        stockPot: ActorRef,
        recordDelivery: RecordDelivery,
    ): StockPotProtocol.Reply {
        // TODO: Consider adding a timeout here
        return logger.wrapMethod("Recording delivery with requestId=${recordDelivery.requestId}") {
            stockPot.ask(recordDelivery).getOrThrow()
        }
    }
}
