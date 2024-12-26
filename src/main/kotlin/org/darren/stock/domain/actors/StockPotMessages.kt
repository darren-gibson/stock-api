package org.darren.stock.domain.actors

import org.darren.stock.domain.Location

typealias Reply = Result<Double>

sealed class StockPotMessages {
    abstract suspend fun execute(location: Location, productId: String, currentStock: Double): Double
}