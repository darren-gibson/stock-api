package org.darren.stock.ktor.idempotency

/**
 * Stored response for idempotent requests.
 */
data class IdempotentResponse(
    val statusCode: Int,
    val body: String,
    val contentType: String,
    val timestamp: Long,
    val bodyHash: String, // SHA-256 hash of the request body for content validation
)

/**
 * Store for idempotency keys and cached responses.
 * Implementations may use in-memory storage (for development/testing)
 * or persistent storage like Redis (for production).
 */
interface IdempotencyStore {
    /**
     * Check if a requestId already exists and return the cached response.
     * Returns null if not found or if the entry has expired.
     */
    fun get(requestId: String): IdempotentResponse?

    /**
     * Store a response for a requestId.
     * @param bodyHash SHA-256 hash of the request body for content validation
     */
    fun put(
        requestId: String,
        statusCode: Int,
        body: String,
        contentType: String = "application/json",
        bodyHash: String,
    )

    /**
     * Remove expired entries (can be called periodically for cleanup).
     */
    fun cleanup()
}
