package com.darren.stock.domain.handlers

import com.darren.stock.domain.OperationNotSupportedException
import com.darren.stock.domain.actors.TrackedStockPotMessages
import java.time.LocalDateTime

class SaleHandler(private val helper: HandlerHelper) {
    suspend fun sale(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
        when (val type = helper.getStockPot(locationId, productId)) {
            is ChannelType.TrackedChannel -> type.channel.send(TrackedStockPotMessages.SaleEvent(eventTime, quantity))
            else -> throw OperationNotSupportedException("Untracked location $locationId cannot perform sales.")
        }
    }
}