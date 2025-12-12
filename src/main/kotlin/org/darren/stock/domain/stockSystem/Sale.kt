package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.messages.RecordSale
import java.time.LocalDateTime

object Sale {
    suspend fun StockSystem.recordSale(
        locationId: String,
        productId: String,
        quantity: Double,
        eventTime: LocalDateTime,
    ) {
        locations.ensureLocationsAreTracked(locationId)
        val stockPot = getStockPot(locationId, productId)
        val result = CompletableDeferred<Reply>()
        stockPot.send(RecordSale(eventTime, quantity, result))
        result.await().getOrThrow()
    }
}
