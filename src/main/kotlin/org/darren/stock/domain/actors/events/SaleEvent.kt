package org.darren.stock.domain.actors.events

import kotlinx.serialization.Serializable
import org.darren.stock.domain.StockState
import org.darren.stock.util.DateSerializer
import java.time.LocalDateTime

@Serializable
class SaleEvent(
    @Serializable(with = DateSerializer::class)
    override val eventDateTime: LocalDateTime,
    val quantity: Double,
    override val requestId: String,
    override val contentHash: String,
) : StockPotEvent() {
    override suspend fun apply(state: StockState): StockState {
        val newQuantity = state.quantity!! - quantity
        if (newQuantity < 0) {
            return state.copy(
                quantity = 0.0,
                pendingAdjustment = state.pendingAdjustment + newQuantity,
                lastUpdated = eventDateTime,
            )
        }

        return state.copy(quantity = newQuantity, lastUpdated = eventDateTime, lastRequestId = requestId)
    }

    override fun toString(): String = "SaleEvent(eventDateTime=$eventDateTime, quantity=$quantity, requestId='$requestId', contentHash='$contentHash')"
}
