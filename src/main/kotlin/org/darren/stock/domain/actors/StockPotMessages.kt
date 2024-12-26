package org.darren.stock.domain.actors

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.Location

typealias Reply = Result<Double>

sealed class StockPotMessages(val result: CompletableDeferred<Reply>) {
    abstract suspend fun execute(location: Location, productId: String, currentStock: Double): Double
}