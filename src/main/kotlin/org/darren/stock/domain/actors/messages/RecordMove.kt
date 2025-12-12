package org.darren.stock.domain.actors.messages

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.InsufficientStockException
import org.darren.stock.domain.MovementReason
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.events.MoveEvent
import org.darren.stock.domain.actors.events.StockPotEvent
import java.time.LocalDateTime

class RecordMove(
    val quantity: Double,
    val to: SendChannel<StockPotMessages>,
    val reason: MovementReason,
    private val eventTime: LocalDateTime,
    result: CompletableDeferred<Reply>,
) : StockPotMessages(result) {
    override suspend fun validate(state: StockState): StockPotEvent {
        val stockState = performMove(state).getOrThrow()
        return MoveEvent(quantity, stockState.location.id, reason, eventTime)
    }

    private suspend fun performMove(state: StockState): Reply {
        if (state.quantity!! < quantity) {
            throw InsufficientStockException()
        }

        val internalMoveResult = CompletableDeferred<Reply>()
        to.send(RecordInternalMoveTo(state.productId, quantity, state.location.id, reason, eventTime, internalMoveResult))
        return internalMoveResult.await()
    }

    override fun toString(): String = "RecordMove(quantity=$quantity, to=$to, reason=$reason, eventTime=$eventTime)"
}
