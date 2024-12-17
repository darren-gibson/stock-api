package org.darren.stock.persistence

import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.actors.TrackedStockPotMessages

class InMemoryStockEventRepository: StockEventRepository {
    override fun getEventsForProductLocation(location: String, product: String): Iterable<TrackedStockPotMessages> {
        TODO("Not yet implemented")
    }

    override fun insert(location: String, product: String, event: TrackedStockPotMessages) {
        TODO("Not yet implemented")
    }
}