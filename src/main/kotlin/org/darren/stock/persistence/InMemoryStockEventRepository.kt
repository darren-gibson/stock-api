package org.darren.stock.persistence

import org.darren.stock.domain.IdempotencyStatus
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

    override fun getEventsByRequestId(requestId: String): Iterable<StockPotEvent> =
        events.values
            .flatten()
            .filter { getIdempotencyData(it)?.first == requestId }
            .map { it }

    override fun checkIdempotencyStatus(
        requestId: String,
        contentHash: String,
    ): IdempotencyStatus {
        val allEvents = events.values.flatten()
        val idempotentEvents = allEvents.filter { it.requestId.isNotEmpty() }
        val matchingRequestIdEvents = idempotentEvents.filter { it.requestId == requestId }

        return when {
            matchingRequestIdEvents.isEmpty() -> IdempotencyStatus.NOT_FOUND
            matchingRequestIdEvents.any { it.contentHash == contentHash } -> IdempotencyStatus.MATCH
            else -> IdempotencyStatus.CONTENT_MISMATCH
        }
    }

    private fun getIdempotencyData(event: StockPotEvent): Pair<String, String>? = event.requestId to event.contentHash
}
