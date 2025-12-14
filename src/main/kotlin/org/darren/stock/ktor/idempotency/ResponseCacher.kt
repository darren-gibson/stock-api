package org.darren.stock.ktor.idempotency

import java.util.concurrent.atomic.AtomicLong

/**
 * ResponseCacher provides a small abstraction over the IdempotencyStore and
 * exposes simple cache metrics (hit/miss counters) for observability.
 */
interface ResponseCacher {
    fun get(requestId: String): IdempotentResponse?

    fun store(
        requestId: String,
        statusCode: Int,
        body: String,
        contentType: String,
        bodyHash: String,
    )

    /**
     * Number of cache hits observed by this cacher.
     */
    fun hitCount(): Long

    /**
     * Number of cache misses observed by this cacher.
     */
    fun missCount(): Long
}

class DefaultResponseCacher(
    private val store: IdempotencyStore,
) : ResponseCacher {
    private val hits = AtomicLong(0)
    private val misses = AtomicLong(0)

    override fun get(requestId: String): IdempotentResponse? {
        val value = store.get(requestId)
        if (value != null) {
            hits.incrementAndGet()
        } else {
            misses.incrementAndGet()
        }
        return value
    }

    override fun store(
        requestId: String,
        statusCode: Int,
        body: String,
        contentType: String,
        bodyHash: String,
    ) {
        store.put(requestId, statusCode, body, contentType, bodyHash)
    }

    override fun hitCount(): Long = hits.get()

    override fun missCount(): Long = misses.get()
}
