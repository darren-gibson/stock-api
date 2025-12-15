package org.darren.stock.domain.service

import kotlinx.coroutines.runBlocking
import org.darren.stock.domain.Location
import org.darren.stock.domain.StockLevel
import org.darren.stock.domain.StockState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class StockServiceTest {
    @Test
    fun `getStockResponse maps values and children correctly`() {
        val now = LocalDateTime.of(2024, 12, 13, 12, 0)

        val childState = StockState(Location("child-1"), "product-1", 5.0, 0.0, now)
        val childLevel = StockLevel(childState, emptyList())

        val rootState = StockState(Location("root-1"), "product-1", 10.0, 1.0, now)
        val rootLevel = StockLevel(rootState, listOf(childLevel))

        val fakeReader =
            object : StockReader {
                override suspend fun retrieveValue(
                    locationId: String,
                    productId: String,
                    includeChildren: Boolean,
                ): StockLevel = rootLevel
            }

        val fakeValidator =
            object : LocationValidator {
                override suspend fun ensureValidLocation(locationId: String) {
                    // no-op for test
                }
            }

        val service = StockService(fakeReader, fakeValidator)

        val response = runBlocking { service.getStockResponse("root-1", "product-1", true) }

        assertEquals("root-1", response.locationId)
        assertEquals("product-1", response.productId)
        assertEquals(10.0, response.quantity ?: error("quantity is null"))
        assertEquals(1.0, response.pendingAdjustment)
        assertEquals(1, response.childLocations.size)
        // totalQuantity = root quantity + child total
        assertEquals(15.0, response.totalQuantity ?: error("totalQuantity is null"))
    }
}
