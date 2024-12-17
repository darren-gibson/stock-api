package org.darren.stock.domain.actors

import org.darren.stock.domain.StockCountReason
import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDateTime

abstract class UntrackedStockPotMessages {
    class GetValue(val response: CompletableDeferred<Double>) : UntrackedStockPotMessages() {
        override fun toString(): String {
            return "GetValue(response=$response)"
        }
    }

    class CountEvent(val eventTime: LocalDateTime, val quantity: Double, val reason: StockCountReason) :
        UntrackedStockPotMessages() {
        override fun toString(): String {
            return "CountEvent(eventTime=$eventTime, quantity=$quantity, reason=$reason)"
        }
    }
}