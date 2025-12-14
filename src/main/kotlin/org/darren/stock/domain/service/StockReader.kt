package org.darren.stock.domain.service

import org.darren.stock.domain.StockLevel
import org.darren.stock.domain.stockSystem.GetValue.retrieveValue
import org.darren.stock.domain.stockSystem.StockSystem

/** Adapter abstraction to read stock values. This enables injecting a test-friendly
 * implementation instead of relying on the global extension-based retrieval logic. */
interface StockReader {
    suspend fun retrieveValue(
        locationId: String,
        productId: String,
        includeChildren: Boolean,
    ): StockLevel
}

class StockSystemReader(
    private val stockSystem: StockSystem,
) : StockReader {
    override suspend fun retrieveValue(
        locationId: String,
        productId: String,
        includeChildren: Boolean,
    ): StockLevel = stockSystem.retrieveValue(locationId, productId, includeChildren)
}
