package org.darren.stock.domain.stockSystem

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
        val result = stockPot.ask(StockPotProtocol.RecordSale(eventTime, quantity))
        result.getOrThrow().result
    }
}
