package org.darren.stock.domain

import org.darren.stock.domain.actors.TrackedStockPotMessages

interface StockEventRepository {
    fun getEventsForProductLocation(location: String, product : String): Iterable<TrackedStockPotMessages>
    fun insert(location: String, product : String, event: TrackedStockPotMessages)
}