package org.darren.stock.domain.actors

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.Location

class GetValue(val response: CompletableDeferred<Double>) : StockPotMessages() {
    override suspend fun execute(location: Location, productId: String, currentStock: Double): Double {
        response.complete(currentStock)
        return currentStock
    }

    override fun toString(): String {
        return "GetValue(response=$response)"
    }
}