package org.darren.stock.domain.actors.messages

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.events.StockPotEvent

sealed class StockPotMessages(val result: CompletableDeferred<Reply>) {
    abstract suspend fun validate(state: StockState): StockPotEvent
}