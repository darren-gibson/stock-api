package org.darren.stock.domain.actors.events

import kotlinx.serialization.Serializable
import org.darren.stock.domain.StockState
import org.darren.stock.ktor.DateSerializer
import java.time.LocalDateTime

@Serializable
class OverrideStockLevelEvent(
    val quantity: Double,
    val pendingAdjustment: Double,
    @Serializable(with = DateSerializer::class)
    override val eventDateTime: LocalDateTime,
) : StockPotEvent() {
    override suspend fun apply(state: StockState): StockState = state.copy(quantity = quantity, pendingAdjustment = pendingAdjustment)
}
