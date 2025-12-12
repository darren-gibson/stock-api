package org.darren.stock.domain

import org.darren.stock.domain.actors.events.StockPotEvent

interface StockEventRepository {
    fun getEvents(
        location: String,
        product: String,
    ): Iterable<StockPotEvent>

    fun insert(
        location: String,
        product: String,
        event: StockPotEvent,
    )
}
