package org.darren.stock.domain.actors.messages

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.MovementReason
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.events.InternalMoveToEvent
import java.time.LocalDateTime

internal class RecordInternalMoveTo(
    val productId: String,
    val quantity: Double,
    val from: String,
    val reason: MovementReason,
    private val eventTime: LocalDateTime, result: CompletableDeferred<Reply>
) :
    StockPotMessages(result) {

    override suspend fun validate(state: StockState) = InternalMoveToEvent(productId, quantity, from, reason, eventTime)

    override fun toString(): String {
        return "RecordInternalMoveTo(eventTime=$eventTime, productId='$productId', quantity=$quantity, from='$from', reason=$reason)"
    }

}