package org.darren.stock.domain.stockSystem

import arrow.resilience.retry
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.ProductQuantity
import org.darren.stock.domain.RetriableException
import org.darren.stock.domain.actors.StockPotProtocol
import org.darren.stock.domain.actors.StockPotProtocol.RecordDelivery
import org.darren.stock.util.LoggingHelper.logOperation
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

    // Simplified recordDelivery: validate up-front, launch parallel delivery operations,
    // inline small helpers to keep flow linear and easy to follow.
    suspend fun StockSystem.recordDelivery(deliveryRequest: DeliveryRequest) =
        coroutineScope {
            validateRequest(deliveryRequest, locations)

            deliveryRequest.products
                .mapIndexed { index, product ->
                    async {
                        val stockPot = getStockPot(deliveryRequest.locationId, product.productId)

                        val recordDelivery =
                            RecordDelivery(
                                product.quantity,
                                deliveryRequest.supplierId,
                                deliveryRequest.supplierRef,
                                deliveryRequest.deliveryDate,
                                "${deliveryRequest.requestId}-$index",
                            )

                        // Retry only retriable failures and log the actual ask operation inline.
                        retryPolicy.retry<RetriableException, StockPotProtocol.Reply> {
                            logger.logOperation("Recording delivery with requestId=${recordDelivery.requestId}") {
                                stockPot.ask(recordDelivery).getOrThrow()
                            }
                        }
                    }
                }.awaitAll()
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
}
