package org.darren.stock.domain.actors

import org.darren.stock.domain.Location
import java.time.LocalDateTime

class DeliveryEvent(val eventTime: LocalDateTime, val quantity: Double) : StockPotMessages() {
    override suspend fun execute(location: Location, productId: String, currentStock: Double) =
        currentStock + quantity


    override fun toString(): String {
        return "DeliveryEvent(eventTime=$eventTime, quantity=$quantity)"
    }
}