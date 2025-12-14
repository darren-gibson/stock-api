package org.darren.stock.persistence

import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.actors.events.StockPotEvent

class InMemoryStockEventRepository : StockEventRepository {
    private val events = mutableMapOf<Pair<String, String>, MutableList<StockPotEvent>>()

    override fun getEvents(
        location: String,
        product: String,
    ): Iterable<StockPotEvent> {
        val eventsInSequenceTheyOccurred = events[Pair(location, product)] ?: emptyList()
        return eventsInSequenceTheyOccurred
            .mapIndexed { i, e -> Pair(Pair(i, e.eventDateTime), e) }
            .sortedWith(compareBy({ it.first.second }, { it.first.first }))
            .map { it.second }
    }

    override fun insert(
        location: String,
        product: String,
        event: StockPotEvent,
    ) {
        events.getOrPut(Pair(location, product)) { mutableListOf() }.add(event)
    }
}
