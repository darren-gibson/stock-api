package org.darren.stock.domain

import org.darren.stock.domain.actors.events.StockPotMessages

interface StockEventRepository {
    fun getEventsForProductLocation(location: String, product : String): Iterable<StockPotMessages>
    fun insert(location: String, product : String, event: StockPotMessages)
}