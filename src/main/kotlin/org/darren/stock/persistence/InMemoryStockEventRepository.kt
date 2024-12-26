package org.darren.stock.persistence

import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.actors.StockPotMessages

class InMemoryStockEventRepository: StockEventRepository {
    override fun getEventsForProductLocation(location: String, product: String): Iterable<StockPotMessages> {
        TODO("Not yet implemented")
    }

    override fun insert(location: String, product: String, event: StockPotMessages) {
        TODO("Not yet implemented")
    }
}