package org.darren.stock.domain.stockSystem

import io.github.smyrgeorge.actor4k.actor.ref.ActorRef
import org.darren.stock.domain.ProductQuantity
import org.darren.stock.domain.actors.messages.StockPotProtocol
import java.time.LocalDateTime

object Delivery {
    suspend fun StockSystem.recordDelivery(
        locationId: String,
        supplierId: String,
        supplierRef: String,
        deliveryDate: LocalDateTime,
        products: List<ProductQuantity>,
    ) {
        locations.ensureLocationsAreTracked(locationId)
        products.forEach {
            val stockPot = getStockPot(locationId, it.productId)
            val result = processDelivery(stockPot, it.quantity, supplierId, supplierRef, deliveryDate)
            result.getOrThrow().result
        }
        // TODO: These calls should be async
        // TODO: What happens if the delivery fails?
    }

    private suspend fun processDelivery(
        stockPot: ActorRef,
        quantity: Double,
        supplierId: String,
        supplierRef: String,
        deliveryDate: LocalDateTime,
    ): Result<StockPotProtocol.Reply> =
        stockPot.ask(
            StockPotProtocol.RecordDelivery(quantity, supplierId, supplierRef, deliveryDate),
        )
}
