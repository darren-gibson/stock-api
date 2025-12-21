package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.actors.messages.StockPotProtocol.RecordCount
import java.time.LocalDateTime

suspend fun StockSystem.count(
    location: String,
    product: String,
    quantity: Double,
    reason: StockCountReason,
    eventTime: LocalDateTime,
) {
    val stockPot = getStockPot(location, product)
    val reply = stockPot.ask(RecordCount(eventTime, quantity, reason)).getOrThrow()

    reply.result
}
