package org.darren.stock.domain.actors

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.Location
import org.darren.stock.domain.OperationNotSupportedException
import java.time.LocalDateTime

class SaleEvent(val eventTime: LocalDateTime, val quantity: Double, val result: CompletableDeferred<Reply>) :
    StockPotMessages() {
    override suspend fun execute(location: Location, productId: String, currentStock: Double): Double {
        val response = Reply.runCatching {
            performSale(location, currentStock)
        }
        result.complete(response)
        return response.getOrElse { currentStock }
    }

    private suspend fun performSale(location: Location, currentStock: Double): Double {
        if (location.isShop()) {
            return currentStock - quantity
        }
        throw OperationNotSupportedException("Location '${location.id}' is not a shop")
    }

    override fun toString(): String {
        return "SaleEvent(eventTime=$eventTime, quantity=$quantity)"
    }
}