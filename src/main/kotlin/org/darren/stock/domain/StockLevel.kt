package org.darren.stock.domain

import java.time.LocalDateTime

data class StockLevel(
    val state: StockState,
    val childLocations: List<StockLevel> = emptyList()
) {
    val totalQuantity: Double
        get() = (state.quantity ?: 0.0) + childLocations.sumOf { it.totalQuantity }
    val quantity: Double?
        get() = state.quantity
    val lastUpdated: LocalDateTime
        get() = state.lastUpdated
    val locationId: String
        get() = state.location.id
    val pendingAdjustment: Double
        get() = state.pendingAdjustment
}
