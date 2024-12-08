package com.darren.stock.domain.actors

import com.darren.stock.domain.MoveResult
import com.darren.stock.domain.StockCountReason
import com.darren.stock.domain.StockMovementReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import java.time.LocalDateTime

sealed class TrackedStockPotMessages : UntrackedStockPotMessages() {
    class SaleEvent(val eventTime: LocalDateTime, val quantity: Double) : TrackedStockPotMessages() {
        override fun toString(): String {
            return "SaleEvent(eventTime=$eventTime, quantity=$quantity)"
        }
    }

    class DeliveryEvent(val eventTime: LocalDateTime, val quantity: Double) : TrackedStockPotMessages() {
        override fun toString(): String {
            return "DeliveryEvent(eventTime=$eventTime, quantity=$quantity)"
        }
    }

    class GetValue(val response: CompletableDeferred<Double>) : TrackedStockPotMessages() {
        override fun toString(): String {
            return "GetValue(response=$response)"
        }
    }

    class CountEvent(val eventTime: LocalDateTime, val quantity: Double, val reason: StockCountReason) :
        TrackedStockPotMessages() {
        override fun toString(): String {
            return "CountEvent(eventTime=$eventTime, quantity=$quantity, reason=$reason)"
        }
    }

    class MoveEvent(
        val productId: String,
        val quantity: Double,
        val to: SendChannel<TrackedStockPotMessages>,
        val reason: StockMovementReason,
        val eventTime: LocalDateTime,
        val result: CompletableDeferred<MoveResult>
    ) : TrackedStockPotMessages() {
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
    ) : TrackedStockPotMessages() {
        override fun toString(): String {
            return "InternalMoveToEvent(productId='$productId', quantity=$quantity, from=$from, reason=$reason, eventTime=$eventTime)"
        }
    }
}