package org.darren.stock.domain.actors.events

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.InsufficientStockException
import org.darren.stock.domain.StockMovementReason
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import java.time.LocalDateTime

class MoveEvent(
    val quantity: Double,
    val to: SendChannel<StockPotMessages>,
    val reason: StockMovementReason,
    private val eventTime: LocalDateTime,
    result: CompletableDeferred<Reply>
) : StockPotMessages(result) {

    private suspend fun performMove(state: StockState): StockState {
        if (state.quantity < quantity)
            throw InsufficientStockException()

        val internalMoveResult = CompletableDeferred<Reply>()
            to.send(InternalMoveToEvent(state.productId, quantity, state.location.id, reason, eventTime, internalMoveResult))
        internalMoveResult.await().getOrThrow()
        return state.copy(quantity = state.quantity - quantity, lastUpdated = eventTime)
    }

    override suspend fun execute(state: StockState): StockState {
        val response = Result.runCatching {
            performMove(state)
        }
        result.complete(response)
        return response.getOrElse { state }
    }

    override fun toString(): String {
        return "MoveEvent(quantity=$quantity, to=$to, reason=$reason, eventTime=$eventTime)"
    }
}