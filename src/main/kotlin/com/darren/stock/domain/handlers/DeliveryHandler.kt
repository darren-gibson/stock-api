package com.darren.stock.domain.handlers

import com.darren.stock.domain.OperationNotSupportedException
import com.darren.stock.domain.actors.TrackedStockPotMessages
import java.time.LocalDateTime

class DeliveryHandler(private val helper: HandlerHelper) {
    suspend fun delivery(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
        when (val type = helper.getStockPot(locationId, productId)) {
            is ChannelType.TrackedChannel -> type.channel.send(
                TrackedStockPotMessages.DeliveryEvent(eventTime, quantity)
            )

            else -> throw OperationNotSupportedException("Untracked location $locationId cannot accept deliveries.")
        }
    }
}