package com.darren.stock.domain.stockSystem

import com.darren.stock.domain.StockCountReason
import com.darren.stock.domain.actors.TrackedStockPotMessages
import com.darren.stock.domain.actors.UntrackedStockPotMessages
import java.time.LocalDateTime

suspend fun StockSystem.count(
    location: String,
    product: String,
    quantity: Double,
    reason: StockCountReason,
    eventTime: LocalDateTime
) {
    when (val type = getStockPot(location, product)) {
        is ChannelType.TrackedChannel -> type.channel.send(
            TrackedStockPotMessages.CountEvent(
                eventTime,
                quantity,
                reason
            )
        )

        is ChannelType.UntrackedChannel -> type.channel.send(
            UntrackedStockPotMessages.CountEvent(
                eventTime,
                quantity,
                reason
            )
        )
    }
}