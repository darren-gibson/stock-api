package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.stockSystem.ChannelType.*
import org.darren.stock.domain.actors.TrackedStockPotMessages as TSPM
import org.darren.stock.domain.actors.UntrackedStockPotMessages as USPM
import java.time.LocalDateTime

suspend fun StockSystem.count(
    location: String,
    product: String,
    quantity: Double,
    reason: StockCountReason,
    eventTime: LocalDateTime
) {
    when (val type = getStockPot(location, product)) {
        is TrackedChannel -> type.channel.send(TSPM.CountEvent(eventTime, quantity, reason))
        is UntrackedChannel -> type.channel.send(USPM.CountEvent(eventTime, quantity, reason))
    }
}