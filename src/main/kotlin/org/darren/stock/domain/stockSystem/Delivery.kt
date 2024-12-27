package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.ProductQuantity
import org.darren.stock.domain.actors.events.DeliveryEvent
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.events.StockPotMessages
import java.time.LocalDateTime

object Delivery {
    suspend fun StockSystem.delivery(
        locationId: String, supplierId: String, supplierRef: String, deliveryDate: LocalDateTime, products: List<ProductQuantity>) {

        val deferredList = products.map {
            val stockPot = getStockPot(locationId, it.productId)
            delivery(stockPot, it.quantity, supplierId, supplierRef, deliveryDate)
        }
        deferredList.awaitAll()

        // else -> throw OperationNotSupportedException("Untracked location $locationId cannot accept deliveries.")
    }

    private suspend fun delivery(
        stockPot: SendChannel<StockPotMessages>,
        quantity: Double,
        supplierId: String,
        supplierRef: String,
        deliveryDate: LocalDateTime
    ): CompletableDeferred<Reply> {
        val result = CompletableDeferred<Reply>()
        stockPot.send(DeliveryEvent(quantity, supplierId, supplierRef, deliveryDate, result))
        return result
    }
}