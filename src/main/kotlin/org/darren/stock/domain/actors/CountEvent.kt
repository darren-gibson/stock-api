package org.darren.stock.domain.actors

import org.darren.stock.domain.Location
import org.darren.stock.domain.StockCountReason
import java.time.LocalDateTime

class CountEvent(val eventTime: LocalDateTime, val quantity: Double, val reason: StockCountReason) :
    StockPotMessages() {
    override suspend fun execute(location: Location, productId: String, currentStock: Double) = quantity

    override fun toString(): String {
        return "CountEvent(eventTime=$eventTime, quantity=$quantity, reason=$reason)"
    }
}