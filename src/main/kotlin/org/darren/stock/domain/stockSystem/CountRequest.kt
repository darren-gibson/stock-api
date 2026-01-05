package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.actors.StockPotProtocol.RecordCount
import java.time.LocalDateTime

data class CountRequest(
    val location: String,
    val product: String,
    val quantity: Double,
    val reason: StockCountReason,
    val eventTime: LocalDateTime,
    val requestId: String,
)

suspend fun StockSystem.count(request: CountRequest) =
    run {
        val recordCount = RecordCount(request.eventTime, request.quantity, request.reason, request.requestId)

        val stockPot = getStockPot(request.location, request.product)
        val reply = stockPot.ask(recordCount).getOrThrow()

        // Return result if present, otherwise null
        reply.result
    }
