package com.darren.stock.domain

import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDateTime

sealed class StockMessages {
    class SaleEvent(val locationId: String, val productId: String, val eventTime: LocalDateTime, val quantity: Double) : StockMessages()
    class DeliveryEvent(val locationId: String, val productId: String, val eventTime: LocalDateTime, val quantity: Double) : StockMessages()
    class GetValue(val locationId: String, val productId: String, val deferred: CompletableDeferred<Double>) : StockMessages()
}
