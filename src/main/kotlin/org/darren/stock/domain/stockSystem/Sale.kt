package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.events.SaleEvent
import java.time.LocalDateTime

object Sale {
    suspend fun StockSystem.sale(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
        val stockPot = getStockPot(locationId, productId)
        val result = CompletableDeferred<Reply>()
        stockPot.send(SaleEvent(eventTime, quantity, result))
        result.await().getOrThrow()
    }
}