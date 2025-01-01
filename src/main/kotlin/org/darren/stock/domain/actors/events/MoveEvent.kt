package org.darren.stock.domain.actors.events

import kotlinx.serialization.Serializable
import org.darren.stock.domain.MovementReason
import org.darren.stock.domain.StockState
import org.darren.stock.ktor.DateSerializer
import java.time.LocalDateTime

@Serializable
class MoveEvent(
    val quantity: Double,
    val to: String,
    val reason: MovementReason,
    @Serializable(with = DateSerializer::class)
    override val eventDateTime: LocalDateTime
) : StockPotEvent() {

    override suspend fun apply(state: StockState) =
        state.copy(quantity = state.quantity - quantity, lastUpdated = eventDateTime)

    override fun toString(): String {
        return "MoveEvent(eventDateTime=$eventDateTime, quantity=$quantity, to=$to, reason=$reason)"
    }
}