package org.darren.stock.domain.actors.messages

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.events.NullStockPotEvent

class GetValue(result: CompletableDeferred<Reply>) : StockPotMessages(result) {
    override suspend fun validate(state: StockState) = NullStockPotEvent()

    override fun toString(): String {
        return "GetValue()"
    }
}