package org.darren.stock.domain.actors

import org.darren.stock.domain.MoveResult
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.StockMovementReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import java.time.LocalDateTime

typealias Reply = Result<Double>

sealed class StockPotMessages {
    class SaleEvent(val eventTime: LocalDateTime, val quantity: Double, val result: CompletableDeferred<Reply>) :
        StockPotMessages() {
        override fun toString(): String {
            return "SaleEvent(eventTime=$eventTime, quantity=$quantity)"
        }
    }

    class DeliveryEvent(val eventTime: LocalDateTime, val quantity: Double) : StockPotMessages() {
        override fun toString(): String {
            return "DeliveryEvent(eventTime=$eventTime, quantity=$quantity)"
        }
    }

    class GetValue(val response: CompletableDeferred<Double>) : StockPotMessages() {
        override fun toString(): String {
            return "GetValue(response=$response)"
        }
    }

    class CountEvent(val eventTime: LocalDateTime, val quantity: Double, val reason: StockCountReason) :
        StockPotMessages() {
        override fun toString(): String {
            return "CountEvent(eventTime=$eventTime, quantity=$quantity, reason=$reason)"
        }
    }

    class MoveEvent(
        val productId: String,
        val quantity: Double,
        val to: SendChannel<StockPotMessages>,
        val reason: StockMovementReason,
        val eventTime: LocalDateTime,
        val result: CompletableDeferred<MoveResult>
    ) : StockPotMessages() {
        override fun toString(): String {
            return "MoveEvent(productId='$productId', quantity=$quantity, to=$to, reason=$reason, eventTime=$eventTime)"
        }
    }

    internal class InternalMoveToEvent(
        val productId: String,
        val quantity: Double,
        val from: String,
        val reason: StockMovementReason,
        val eventTime: LocalDateTime
    ) : StockPotMessages() {
        override fun toString(): String {
            return "InternalMoveToEvent(productId='$productId', quantity=$quantity, from=$from, reason=$reason, eventTime=$eventTime)"
        }
    }
}