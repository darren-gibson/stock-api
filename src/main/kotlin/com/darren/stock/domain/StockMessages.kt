package com.darren.stock.domain

import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDateTime

sealed class StockMessages(val locationId: String, val productId: String) {
    class SetStockLevelEvent(locationId: String, productId: String, val eventTime: LocalDateTime, val quantity: Double) :
        StockMessages(locationId, productId) {
        override fun toString(): String {
            return "SetStockLevelEvent(locationId=$locationId, productId=$productId, eventTime=$eventTime, quantity=$quantity)"
        }
    }

    class SaleEvent(locationId: String, productId: String, val eventTime: LocalDateTime, val quantity: Double) :
        StockMessages(locationId, productId) {
        override fun toString(): String {
            return "SaleEvent(locationId=$locationId, productId=$productId, eventTime=$eventTime, quantity=$quantity)"
        }
    }

    class DeliveryEvent(locationId: String, productId: String, val eventTime: LocalDateTime, val quantity: Double) :
        StockMessages(locationId, productId) {
        override fun toString(): String {
            return "DeliveryEvent(locationId=$locationId, productId=$productId, eventTime=$eventTime, quantity=$quantity)"
        }
    }

    class GetValue(locationId: String, productId: String, val deferred: CompletableDeferred<Double>) :
        StockMessages(locationId, productId) {
        override fun toString(): String {
            return "GetValue(locationId=$locationId, productId=$productId, deferred=$deferred)"
        }
    }
}
