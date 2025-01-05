package org.darren.stock.domain.actors.messages

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.events.SaleEvent
import org.darren.stock.domain.actors.events.StockPotEvent
import java.time.LocalDateTime

class RecordSale(private val eventTime: LocalDateTime, val quantity: Double, result: CompletableDeferred<Reply>) :
    StockPotMessages(result) {

    override suspend fun validate(state: StockState): StockPotEvent = SaleEvent(eventTime, quantity)

    override fun toString(): String {
        return "RecordSale(eventTime=$eventTime, quantity=$quantity)"
    }
}