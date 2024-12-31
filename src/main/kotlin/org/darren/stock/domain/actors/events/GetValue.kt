package org.darren.stock.domain.actors.events

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply

class GetValue(result: CompletableDeferred<Reply>) : StockPotMessages(result) {
    override suspend fun execute(state: StockState) = state

    override fun toString(): String {
        return "GetValue(response=$result)"
    }
}