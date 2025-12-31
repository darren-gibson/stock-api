package org.darren.stock.domain.actors.events

import kotlinx.serialization.Serializable
import org.darren.stock.domain.MovementReason
import org.darren.stock.domain.StockState
import org.darren.stock.util.DateSerializer
import java.time.LocalDateTime

@Serializable
internal class InternalMoveToEvent(
    val productId: String,
    val quantity: Double,
    val from: String,
    val reason: MovementReason,
    @Serializable(with = DateSerializer::class)
    override val eventDateTime: LocalDateTime,
) : StockPotEvent() {
    override val requestId: String = ""
    override val contentHash: String = ""

    override suspend fun apply(state: StockState) = state.copy(quantity = state.quantity!! + quantity, lastUpdated = eventDateTime, lastRequestId = requestId)

    override fun toString(): String = "InternalMoveToEvent(eventDateTime=$eventDateTime, productId='$productId', quantity=$quantity, from='$from', reason=$reason)"
}
