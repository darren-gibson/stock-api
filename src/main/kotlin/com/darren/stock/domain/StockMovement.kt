package com.darren.stock.domain

data class StockMovement(
    val from: String,
    val to: String,
    val product: String,
    val quantity: Double,
    val reason: StockMovementReason
)
