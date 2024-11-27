package com.darren.stock.domain

import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDateTime

sealed class StockPotMessages {
    class SaleEvent(val eventTime: LocalDateTime, val quantity: Double) : StockPotMessages() {
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
}