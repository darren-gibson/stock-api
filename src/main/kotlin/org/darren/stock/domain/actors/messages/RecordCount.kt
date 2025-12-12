package org.darren.stock.domain.actors.messages

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.events.CountEvent
import java.time.LocalDateTime

class RecordCount(
    private val eventTime: LocalDateTime,
    val quantity: Double,
    val reason: StockCountReason,
    result: CompletableDeferred<Reply>,
) : StockPotMessages(result) {
    override suspend fun validate(state: StockState) = CountEvent(eventTime, quantity, reason)

    override fun toString(): String = "RecordCount(eventTime=$eventTime, quantity=$quantity, reason=$reason)"
}
