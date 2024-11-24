package com.darren.stock.domain

import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDateTime

sealed class StockPotMessages {
    class SaleEvent(val eventTime: LocalDateTime, val quantity: Double) : StockPotMessages()
    class DeliveryEvent(val eventTime: LocalDateTime, val quantity: Double) : StockPotMessages()
    class GetValue(val deferred: CompletableDeferred<Double>) : StockPotMessages()
}