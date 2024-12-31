package org.darren.stock.domain.actors.events

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.OperationNotSupportedException
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import java.time.LocalDateTime

class SaleEvent(val eventTime: LocalDateTime, val quantity: Double, result: CompletableDeferred<Reply>) :
    StockPotMessages(result) {

    override suspend fun execute(state: StockState): StockState {
        if (state.location.isShop())
            return performSale(state)

        throw OperationNotSupportedException("Location '${state.location.id}' is not a shop")
    }

    private fun performSale(state: StockState): StockState {
        val newQuantity = state.quantity - quantity
        if (newQuantity < 0) {
            return state.copy(
                quantity = 0.0, pendingAdjustment = state.pendingAdjustment + newQuantity, lastUpdated = eventTime
            )
        }

        return state.copy(quantity = newQuantity, lastUpdated = eventTime)
    }

    override fun toString(): String {
        return "SaleEvent(eventTime=$eventTime, quantity=$quantity)"
    }
}