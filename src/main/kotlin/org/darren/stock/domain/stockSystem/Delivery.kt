package org.darren.stock.domain.stockSystem

import io.github.smyrgeorge.actor4k.actor.ref.ActorRef
import org.darren.stock.domain.ProductQuantity
import org.darren.stock.domain.actors.StockPotProtocol
import org.darren.stock.domain.actors.StockPotProtocol.RecordDelivery
import java.time.LocalDateTime

data class DeliveryRequest(
    val locationId: String,
    val supplierId: String,
    val supplierRef: String,
    val deliveryDate: LocalDateTime,
    val products: List<ProductQuantity>,
    val requestId: String,
)

data class DeliveryDetails(
    val quantity: Double,
    val supplierId: String,
    val supplierRef: String,
    val deliveryDate: LocalDateTime,
    val requestId: String,
)

class IdempotencyContentMismatchException(
    message: String,
    val requestId: String,
) : Exception(message)

object Delivery {
    suspend fun StockSystem.recordDelivery(deliveryRequest: DeliveryRequest) {
        // Calculate content hash for idempotency checking
        val contentHash =
            RecordDelivery(
                quantity = deliveryRequest.products.sumOf { it.quantity },
                supplierId = deliveryRequest.supplierId,
                supplierRef = deliveryRequest.supplierRef,
                eventTime = deliveryRequest.deliveryDate,
                requestId = deliveryRequest.requestId,
            ).contentHash()

        locations.ensureLocationsAreTracked(deliveryRequest.locationId)
        deliveryRequest.products.forEachIndexed { index, product ->
            val stockPot = getStockPot(deliveryRequest.locationId, product.productId)
            val deliveryDetails =
                with(deliveryRequest) {
                    DeliveryDetails(
                        product.quantity,
                        supplierId,
                        supplierRef,
                        deliveryDate,
                        "$requestId-$index",
                    )
                }
            val result = processDelivery(stockPot, deliveryDetails)
            result.getOrThrow().result
        }
        // TODO: These calls should be async
        // TODO: What happens if the delivery fails?
    }

    private suspend fun processDelivery(
        stockPot: ActorRef,
        deliveryDetails: DeliveryDetails,
    ): Result<StockPotProtocol.Reply> =
        stockPot.ask(
            with(deliveryDetails) {
                StockPotProtocol.RecordDelivery(
                    quantity,
                    supplierId,
                    supplierRef,
                    deliveryDate,
                    requestId,
                )
            },
        )
}
