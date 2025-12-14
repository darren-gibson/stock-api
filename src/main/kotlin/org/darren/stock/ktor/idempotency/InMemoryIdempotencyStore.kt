
package org.darren.stock.ktor.idempotency

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit

/**
 * In-memory implementation of IdempotencyStore using Caffeine cache.
 * Thread-safe for concurrent access.
 *
 * This implementation is suitable for:
 * - Development and testing
 * - Single-instance deployments
 * - Low-volume applications
 *
 * For production with multiple instances, use a persistent implementation
 * backed by Redis or similar distributed cache.
 */
class InMemoryIdempotencyStore(
    private val ttlSeconds: Long = 86_400L, // 24 hours
    private val maximumSize: Long = 10_000L,
) : IdempotencyStore {
    private val cache: Cache<String, IdempotentResponse> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
            .maximumSize(maximumSize)
            .build()

    override fun get(requestId: String): IdempotentResponse? = cache.getIfPresent(requestId)

    override fun put(
        requestId: String,
        statusCode: Int,
        body: String,
        contentType: String,
        bodyHash: String,
    ) {
        cache.put(
            requestId,
            IdempotentResponse(
                statusCode = statusCode,
                body = body,
                contentType = contentType,
                timestamp = System.currentTimeMillis() / 1000,
                bodyHash = bodyHash,
            ),
        )
    }

    /**
     * Clear all stored responses.
     * This method is not part of the IdempotencyStore interface as it's only useful
     * for testing and should not be exposed in production implementations.
     */
    fun clear() {
        cache.invalidateAll()
    }

    override fun cleanup() {
        // Caffeine handles expiry automatically, but we can force cleanup if needed
        cache.cleanUp()
    }
}
