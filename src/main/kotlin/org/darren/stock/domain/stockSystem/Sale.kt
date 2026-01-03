package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.actors.StockPotProtocol.RecordSale
import java.time.LocalDateTime

object Sale {
    suspend fun StockSystem.recordSale(
        locationId: String,
        productId: String,
        quantity: Double,
        eventTime: LocalDateTime,
        requestId: String,
    ) {
        val recordSale = RecordSale(eventTime, quantity, requestId)

        locations.ensureLocationsAreTracked(locationId)
        val stockPot = getStockPot(locationId, productId)
        val result =
            stockPot.ask(recordSale)
        result.getOrThrow().result
    }
}
