package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.messages.RecordCount
import java.time.LocalDateTime

suspend fun StockSystem.count(
    location: String, product: String, quantity: Double, reason: StockCountReason, eventTime: LocalDateTime
) {
    val stockPot = getStockPot(location, product)
    val result = CompletableDeferred<Reply>()
    stockPot.send(RecordCount(eventTime, quantity, reason, result))
    result.await().getOrThrow()
}