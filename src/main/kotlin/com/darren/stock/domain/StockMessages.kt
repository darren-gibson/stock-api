package com.darren.stock.domain

import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDateTime

sealed class StockMessages {
    class SaleEvent(val eventTime: LocalDateTime, val locationId: String, val productId: String, val quantity: Double) : StockMessages()
    class DeliveryEvent(val eventTime: LocalDateTime, val locationId: String, val productId: String, val quantity: Double) : StockMessages()
    class GetValue(val locationId: String, val productId: String, val deferred: CompletableDeferred<Double>) : StockMessages()
}