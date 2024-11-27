package com.darren.stock.domain.actors

import kotlinx.coroutines.CompletableDeferred
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
}