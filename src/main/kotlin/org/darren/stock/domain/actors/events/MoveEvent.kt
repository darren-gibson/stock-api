package org.darren.stock.domain.actors.events

import kotlinx.serialization.Serializable
import org.darren.stock.domain.MovementReason
import org.darren.stock.domain.StockState
import org.darren.stock.util.DateSerializer
import java.time.LocalDateTime

@Serializable
class MoveEvent(
    val quantity: Double,
    val to: String,
    val reason: MovementReason,
    @Serializable(with = DateSerializer::class)
    override val eventDateTime: LocalDateTime,
    override val requestId: String,
    override val contentHash: String,
) : StockPotEvent() {
    // Safe: quantity is guaranteed non-null - either initialized to 0.0 or set by prior event
    override suspend fun apply(state: StockState) = state.copy(quantity = state.quantity!! - quantity, lastUpdated = eventDateTime, lastRequestId = requestId)

    override fun toString(): String = "MoveEvent(eventDateTime=$eventDateTime, quantity=$quantity, to=$to, reason=$reason, requestId='$requestId', contentHash='$contentHash')"
}
