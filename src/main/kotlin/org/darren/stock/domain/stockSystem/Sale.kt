package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.OperationNotSupportedException
import org.darren.stock.domain.actors.TrackedStockPotMessages
import java.time.LocalDateTime

suspend fun StockSystem.sale(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
    when (val type = getStockPot(locationId, productId)) {
        is ChannelType.TrackedChannel -> type.channel.send(TrackedStockPotMessages.SaleEvent(eventTime, quantity))
        else -> throw OperationNotSupportedException("Untracked location $locationId cannot perform sales.")
    }
}
