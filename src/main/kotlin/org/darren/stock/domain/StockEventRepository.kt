package org.darren.stock.domain

import org.darren.stock.domain.actors.events.StockPotEvent

interface StockEventRepository : HealthProbe {
    /**
     * Get all events for a location and product in chronological order.
     * Events are returned ordered by eventDateTime.
     * When events have the same eventDateTime, insertion order is maintained.
     */
    suspend fun getEventsInChronologicalOrder(
        location: String,
        product: String,
    ): Iterable<StockPotEvent>

    suspend fun insert(
        location: String,
        product: String,
        event: StockPotEvent,
    )

    suspend fun getEventsByRequestId(requestId: String): Iterable<StockPotEvent>

    /**
     * Get events that occurred after the specified requestId in chronological order.
     * Events are returned ordered by eventDateTime.
     * When events have the same eventDateTime, insertion order is maintained.
     */
    suspend fun getEventsAfterRequestIdInChronologicalOrder(
        location: String,
        product: String,
        afterRequestId: String,
    ): Iterable<StockPotEvent>

    /**
     * Get the requestId of the last persisted event for a location and product.
     * Returns null if no events exist.
     */
    suspend fun getLastPersistedRequestId(
        location: String,
        product: String,
    ): String?

    /**
     * Check the idempotency status for a request.
     * Returns the status indicating whether the request should be processed or not.
     */
    suspend fun checkIdempotencyStatus(
        requestId: String,
        contentHash: String,
    ): IdempotencyStatus
}

enum class IdempotencyStatus {
    /** No events found for this requestId - process normally */
    NOT_FOUND,

    /** Events found with matching content hash - return cached response */
    MATCH,

    /** Events found but content hash differs - reject with conflict */
    CONTENT_MISMATCH,
}

class RepositoryFailureException(
    message: String?,
    cause: Throwable? = null,
) : RetriableException(message, cause)
