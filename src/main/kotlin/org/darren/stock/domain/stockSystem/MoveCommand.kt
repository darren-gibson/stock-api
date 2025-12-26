package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.MovementReason
import java.time.LocalDateTime

/**
 * Parameter object for stock movement operations.
 * Reduces parameter list complexity and improves maintainability.
 */
data class MoveCommand(
    val fromLocationId: String,
    val toLocationId: String,
    val productId: String,
    val quantity: Double,
    val reason: MovementReason,
    val movedAt: LocalDateTime,
    val requestId: String,
)
