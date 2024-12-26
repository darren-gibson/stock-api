package org.darren.stock.domain.actors

import org.darren.stock.domain.Location
import org.darren.stock.domain.StockMovementReason
import java.time.LocalDateTime

internal class InternalMoveToEvent(
    val productId: String,
    val quantity: Double,
    val from: String,
    val reason: StockMovementReason,
    val eventTime: LocalDateTime
) : StockPotMessages() {
    override suspend fun execute(location: Location, productId: String, currentStock: Double) =
        currentStock + quantity

    override fun toString(): String {
        return "InternalMoveToEvent(productId='$productId', quantity=$quantity, from=$from, reason=$reason, eventTime=$eventTime)"
    }
}