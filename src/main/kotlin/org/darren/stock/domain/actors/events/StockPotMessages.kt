package org.darren.stock.domain.actors.events

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply

sealed class StockPotMessages(val result: CompletableDeferred<Reply>) {
    abstract suspend fun execute(state: StockState): StockState
}