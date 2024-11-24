package com.darren.stock.domain

import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDateTime

sealed class StockMessages(val locationId: String, val productId: String) {
    class SaleEvent(locationId: String, productId: String, val eventTime: LocalDateTime, val quantity: Double) :
        StockMessages(locationId, productId)

    class DeliveryEvent(locationId: String, productId: String, val eventTime: LocalDateTime, val quantity: Double) :
        StockMessages(locationId, productId)

    class GetValue(locationId: String, productId: String, val deferred: CompletableDeferred<Double>) :
        StockMessages(locationId, productId)
}
