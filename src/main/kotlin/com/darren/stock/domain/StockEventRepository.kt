package com.darren.stock.domain

import com.darren.stock.domain.actors.TrackedStockPotMessages

interface StockEventRepository {
    fun getEventsForProductLocation(location: String, product : String): Iterable<TrackedStockPotMessages>
    fun insert(location: String, product : String, event: TrackedStockPotMessages)
}