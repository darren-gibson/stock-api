package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.actors.StockPotMessages
import java.time.LocalDateTime

suspend fun StockSystem.delivery(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
    val stockPot = getStockPot(locationId, productId)
    stockPot.send(StockPotMessages.DeliveryEvent(eventTime, quantity))

    // else -> throw OperationNotSupportedException("Untracked location $locationId cannot accept deliveries.")
}
