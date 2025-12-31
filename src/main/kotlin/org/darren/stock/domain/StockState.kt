package org.darren.stock.domain

import java.time.LocalDateTime

data class StockState(
    val location: Location,
    val productId: String,
    val quantity: Double? = 0.0,
    val pendingAdjustment: Double = 0.0,
    val lastUpdated: LocalDateTime = LocalDateTime.MIN,
    val lastRequestId: String? = null,
)
