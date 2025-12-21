package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.messages.RecordCount
import org.darren.stock.domain.actors.messages.StockPotProtocol
import java.time.LocalDateTime
import kotlin.time.toDuration
import kotlin.time.Duration.Companion.seconds
import kotlin.getOrThrow

suspend fun StockSystem.count(
    location: String,
    product: String,
    quantity: Double,
    reason: StockCountReason,
    eventTime: LocalDateTime,
) {
    val stockPot = getStockPot(location, product)
    val reply = stockPot.ask(StockPotProtocol.GetValue(), 1000.seconds)

    reply.getOrThrow()

    // val result = CompletableDeferred<Reply>()
    // stockPot.send(RecordCount(eventTime, quantity, reason, result))
    // result.await().getOrThrow()
}
