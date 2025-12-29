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

    fun getEventsByRequestId(requestId: String): Iterable<StockPotEvent>

    /**
     * Check the idempotency status for a request.
     * Returns the status indicating whether the request should be processed or not.
     */
    fun checkIdempotencyStatus(
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
