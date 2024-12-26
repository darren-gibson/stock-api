package org.darren.stock.domain.actors.events

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.Location
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.actors.Reply
import java.time.LocalDateTime

class CountEvent(val eventTime: LocalDateTime, val quantity: Double, val reason: StockCountReason, result: CompletableDeferred<Reply>) :
    StockPotMessages(result) {
    override suspend fun execute(location: Location, productId: String, currentStock: Double) = quantity

    override fun toString(): String {
        return "CountEvent(eventTime=$eventTime, quantity=$quantity, reason=$reason)"
    }
}