package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.actors.events.DeliveryEvent
import org.darren.stock.domain.actors.Reply
import java.time.LocalDateTime

object Delivery {
    suspend fun StockSystem.delivery(
        locationId: String,
        productId: String,
        quantity: Double,
        eventTime: LocalDateTime
    ) {
        val stockPot = getStockPot(locationId, productId)
        val result = CompletableDeferred<Reply>()
        stockPot.send(DeliveryEvent(eventTime, quantity, result))
        result.await().getOrThrow()

        // else -> throw OperationNotSupportedException("Untracked location $locationId cannot accept deliveries.")
    }
}
