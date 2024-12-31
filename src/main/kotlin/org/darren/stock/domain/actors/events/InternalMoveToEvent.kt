package org.darren.stock.domain.actors.events

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockMovementReason
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import java.time.LocalDateTime

internal class InternalMoveToEvent(
    val productId: String,
    val quantity: Double,
    val from: String,
    val reason: StockMovementReason,
    val eventTime: LocalDateTime, result: CompletableDeferred<Reply>
) :
    StockPotMessages(result) {

    override suspend fun execute(state: StockState) =
        state.copy(quantity = state.quantity + quantity, lastUpdated = eventTime)

    override fun toString(): String {
        return "InternalMoveToEvent(productId='$productId', quantity=$quantity, from=$from, reason=$reason, eventTime=$eventTime)"
    }
}