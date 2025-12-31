package org.darren.stock.persistence

import org.darren.stock.domain.IdempotencyStatus
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.actors.events.StockPotEvent

class InMemoryStockEventRepository : StockEventRepository {
    private val events = mutableMapOf<Pair<String, String>, MutableList<StockPotEvent>>()

    override suspend fun getEventsInChronologicalOrder(
        location: String,
        product: String,
    ): Iterable<StockPotEvent> {
        val eventsInSequenceTheyOccurred = events[Pair(location, product)] ?: emptyList()
        return eventsInSequenceTheyOccurred
            .mapIndexed { i, e -> Pair(Pair(i, e.eventDateTime), e) }
            .sortedWith(compareBy({ it.first.second }, { it.first.first }))
            .map { it.second }
    }

    override suspend fun insert(
        location: String,
        product: String,
        event: StockPotEvent,
    ) {
        events.getOrPut(Pair(location, product)) { mutableListOf() }.add(event)
    }

    override suspend fun getEventsByRequestId(requestId: String): Iterable<StockPotEvent> =
        events.values
            .flatten()
            .filter { getIdempotencyData(it).first == requestId }
            .map { it }

    override suspend fun getEventsAfterRequestIdInChronologicalOrder(
        location: String,
        product: String,
        afterRequestId: String,
    ): Iterable<StockPotEvent> {
        val eventsInSequenceTheyOccurred = events[Pair(location, product)] ?: emptyList()
        val sortedEvents =
            eventsInSequenceTheyOccurred
                .mapIndexed { i, e -> Pair(Pair(i, e.eventDateTime), e) }
                .sortedWith(compareBy({ it.first.second }, { it.first.first }))
                .map { it.second }

        // Find the index of the event with afterRequestId
        val afterIndex = sortedEvents.indexOfFirst { it.requestId == afterRequestId }
        if (afterIndex == -1) {
            // If the event is not found, return all events (shouldn't happen in normal operation)
            return sortedEvents
        }

        // Return events after the found event
        return sortedEvents.drop(afterIndex + 1)
    }

    override suspend fun getLastPersistedRequestId(
        location: String,
        product: String,
    ): String? {
        val eventsInSequenceTheyOccurred = events[Pair(location, product)] ?: emptyList()
        return eventsInSequenceTheyOccurred.lastOrNull()?.requestId
    }

    override suspend fun checkIdempotencyStatus(
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

    private fun getIdempotencyData(event: StockPotEvent): Pair<String, String> = event.requestId to event.contentHash
}
