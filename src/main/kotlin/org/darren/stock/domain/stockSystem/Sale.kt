package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.messages.RecordSale
import org.darren.stock.domain.actors.messages.StockPotProtocol
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
        stockPot.ask( StockPotProtocol.RecordSale(eventTime, quantity))
    }
}
