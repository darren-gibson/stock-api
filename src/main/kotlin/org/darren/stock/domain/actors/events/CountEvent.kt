package org.darren.stock.domain.actors.events

import kotlinx.serialization.Serializable
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.StockState
import org.darren.stock.ktor.DateSerializer
import java.time.LocalDateTime

@Serializable
class CountEvent(
    @Serializable(with = DateSerializer::class)
    override val eventDateTime: LocalDateTime,
    val quantity: Double,
    val reason: StockCountReason
) :
    StockPotEvent() {
    override suspend fun apply(state: StockState) = state.copy(quantity = quantity, lastUpdated = eventDateTime)

    override fun toString(): String {
        return "CountEvent(eventTime=$eventDateTime, quantity=$quantity, reason=$reason)"
    }
}