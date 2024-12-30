package org.darren.stock.domain

data class StockLevel(
    val locationId: String,
    val productId: String,
    val quantity: Double,
    val childLocations: List<StockLevel> = emptyList()
) {
    val totalQuantity: Double
        get() = quantity + childLocations.sumOf { it.totalQuantity }
}
