package com.darren.stock.persistence

import com.darren.stock.domain.StockEventRepository
import com.darren.stock.domain.actors.TrackedStockPotMessages

class InMemoryStockEventRepository: StockEventRepository {
    override fun getEventsForProductLocation(location: String, product: String): Iterable<TrackedStockPotMessages> {
        TODO("Not yet implemented")
    }

    override fun insert(location: String, product: String, event: TrackedStockPotMessages) {
        TODO("Not yet implemented")
    }
}