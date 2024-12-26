package org.darren.stock.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class StockMovementReason {
    @SerialName("replenishment")
    Replenishment,
}