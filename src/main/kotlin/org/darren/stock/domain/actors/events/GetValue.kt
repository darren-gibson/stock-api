package org.darren.stock.domain.actors.events

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.Location
import org.darren.stock.domain.actors.Reply

class GetValue(result: CompletableDeferred<Reply>) : StockPotMessages(result) {
    override suspend fun execute(location: Location, productId: String, currentStock: Double) = currentStock

    override fun toString(): String {
        return "GetValue(response=$result)"
    }
}