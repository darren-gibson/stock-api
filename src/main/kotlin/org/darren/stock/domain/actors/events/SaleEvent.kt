package org.darren.stock.domain.actors.events

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.Location
import org.darren.stock.domain.OperationNotSupportedException
import org.darren.stock.domain.actors.Reply
import java.time.LocalDateTime

class SaleEvent(val eventTime: LocalDateTime, val quantity: Double, result: CompletableDeferred<Reply>) :
    StockPotMessages(result) {
    override suspend fun execute(location: Location, productId: String, currentStock: Double) =
        performSale(location, currentStock)

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