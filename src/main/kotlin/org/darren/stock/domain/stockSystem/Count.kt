package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.actors.CountEvent
import org.darren.stock.domain.actors.Reply
import java.time.LocalDateTime

suspend fun StockSystem.count(
    location: String, product: String, quantity: Double, reason: StockCountReason, eventTime: LocalDateTime
) {
    val stockPot = getStockPot(location, product)
    val result = CompletableDeferred<Reply>()
    stockPot.send(CountEvent(eventTime, quantity, reason, result))
    result.await().getOrThrow()
}