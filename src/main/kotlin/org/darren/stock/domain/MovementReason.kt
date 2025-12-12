package org.darren.stock.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MovementReason {
    @SerialName("replenishment")
    Replenishment,
}
