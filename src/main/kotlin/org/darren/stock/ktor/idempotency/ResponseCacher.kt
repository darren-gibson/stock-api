package org.darren.stock.ktor.idempotency

import java.util.concurrent.atomic.AtomicLong

/**
 * ResponseCacher provides a small abstraction over the `IdempotencyStore` and
 * exposes simple cache metrics (hit/miss counters) for observability.
 *
 * Responsibilities and semantics:
 * - `get(requestId)` performs a read-only lookup and must not mutate the
 *   underlying store. It returns the cached response or `null` when absent.
 * - `store(...)` persists a response for a `requestId`. Callers should only
 *   invoke `store` for successful responses (2xx) — the decision to cache or
 *   not is intentionally left to higher-level routing logic.
 * - Implementations MAY expose in-memory counters (e.g. `hitCount`/`missCount`)
 *   for lightweight metrics. These counters are for observability and do not
 *   change caching semantics.
 *
 * Implementations should document thread-safety guarantees. The provided
 * `DefaultResponseCacher` is thread-safe and keeps counters in-memory.
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

/**
 * Default in-process implementation of `ResponseCacher` that delegates storage
 * operations to an `IdempotencyStore` and keeps local, thread-safe counters
 * for hits and misses.
 *
 * Notes:
 * - Counters are stored in-memory and are not durable across restarts.
 * - This implementation is safe for concurrent access and uses atomics for
 *   counters.
 */
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
