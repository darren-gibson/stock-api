package org.darren.stock.domain.actors.events

import kotlinx.serialization.Serializable
import org.darren.stock.domain.StockState
import org.darren.stock.ktor.DateSerializer
import java.time.LocalDateTime

@Serializable
class SaleEvent(
    @Serializable(with = DateSerializer::class)
    override val eventDateTime: LocalDateTime, val quantity: Double) :
    StockPotEvent() {

    override suspend fun apply(state: StockState): StockState {
        val newQuantity = state.quantity!! - quantity
        if (newQuantity < 0) {
            return state.copy(
                quantity = 0.0, pendingAdjustment = state.pendingAdjustment + newQuantity, lastUpdated = eventDateTime
            )
        }

        return state.copy(quantity = newQuantity, lastUpdated = eventDateTime)
    }

    override fun toString(): String {
        return "SaleEvent(eventDateTime=$eventDateTime, quantity=$quantity)"
    }
}