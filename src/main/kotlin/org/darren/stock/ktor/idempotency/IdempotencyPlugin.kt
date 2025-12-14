package org.darren.stock.ktor.idempotency

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

val logger = KotlinLogging.logger {}

/**
 * Attribute key to mark routes as idempotent.
 */
val IdempotentKey = AttributeKey<Boolean>("Idempotent")

/**
 * Attribute key to store requestId during call processing.
 */
val RequestIdKey = AttributeKey<String>("RequestId")

/**
 * Attribute key to store body hash during call processing.
 */
val BodyHashKey = AttributeKey<String>("BodyHash")

/**
 * Plugin that handles idempotency for state-changing operations.
 * Intercepts requests with requestId and caches responses to prevent duplicate processing.
 *
 * How it works:
 * 1. Before route handler: Check if requestId exists in store
 *    - If found: Return cached response immediately
 *    - If not found: Continue to handler
 * 2. After route handler: Capture response and store it with requestId
 *
 * This keeps domain logic clean - no idempotency concerns in business code.
 */
val IdempotencyPlugin =
    createRouteScopedPlugin(
        name = "IdempotencyPlugin",
    ) {
        // Plugin doesn't need to do anything in hooks anymore
        // The real work is done in the intercept in Route.idempotent()
    }

/**
 * Mark a route as idempotent.
 * This enables automatic response caching and duplicate request detection.
 *
 * Example:
 * ```kotlin
 * route("/locations/{id}/sales") {
 *     idempotent()
 *     post {
 *         // Handler code - no idempotency logic needed
 *     }
 * }
 * ```
 */
fun Route.idempotent() {
    attributes.put(IdempotentKey, true)
    install(IdempotencyPlugin)
}

/**
 * Receive request body and check for idempotent duplicate.
 * This helper receives the body ONCE, extracts requestId from it, checks cache, and returns either:
 * - null if this is a duplicate (cached response already sent)
 * - the deserialized DTO if this is a new request
 *
 * The requestId is now REQUIRED in all state-changing operations. Requests without requestId will fail
 * deserialization with 400 Bad Request, ensuring all operations are safely retryable.
 *
 * Usage:
 * ```kotlin
 * val request = call.receiveAndCheckDuplicate<SaleRequestDTO> { it.requestId } ?: return@post
 * // Continue with business logic using `request`
 * ```
 */
suspend inline fun <reified T : Any> ApplicationCall.receiveAndCheckDuplicate(
    crossinline getRequestId: (T) -> String,
): T? {
    // Receive body ONCE
    val dto = receive<T>()
    val requestId = getRequestId(dto)

    // Compute body hash for content validation
    val json = Json.encodeToString(dto)
    val koin =
        org.koin.java.KoinJavaComponent
            .getKoin()
    val fingerprinter =
        koin.getOrNull(RequestFingerprint::class) ?: DefaultRequestFingerprint()
    val bodyHash = fingerprinter.fingerprint(json)

    // Store requestId and bodyHash for later use by respondIdempotent
    attributes.put(RequestIdKey, requestId)
    attributes.put(BodyHashKey, bodyHash)

    // Check for cached response
    val cacher =
        koin.getOrNull(ResponseCacher::class)
            ?: DefaultResponseCacher(koin.getOrNull(IdempotencyStore::class)!!)
    val cachedResponse = cacher.get(requestId)

    if (cachedResponse != null) {
        // Validate that the request body matches the cached request
        if (cachedResponse.bodyHash != bodyHash) {
            val originalHash = cachedResponse.bodyHash
            logger.warn {
                buildString {
                    append("RequestId $requestId reused with different content. ")
                    append("Original hash: $originalHash, ")
                    append("New hash: $bodyHash")
                }
            }
            response.status(HttpStatusCode.Conflict)
            respond(
                HttpStatusCode.Conflict,
                mapOf(
                    "error" to "Conflict",
                    "message" to "RequestId $requestId has already been used with different request content",
                ),
            )
            return null
        }

        logger.info { "Returning cached response for duplicate requestId: $requestId" }
        response.status(HttpStatusCode.fromValue(cachedResponse.statusCode))
        if (cachedResponse.body.isNotEmpty()) {
            response.headers.append(HttpHeaders.ContentType, cachedResponse.contentType)
            respondText(cachedResponse.body)
        } else {
            respond(HttpStatusCode.fromValue(cachedResponse.statusCode))
        }
        return null // Signal that this was a duplicate
    }

    return dto // Return the DTO for normal processing
}

/**
 * Compute SHA-256 hash of request body for content validation.
 */
fun computeBodyHash(body: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(body.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}

/**
 * Extension to respond with automatic idempotency caching.
 * Use this instead of call.respond() in idempotent routes.
 * Only caches successful responses (2xx) - errors should not be cached.
 */
suspend inline fun <reified T : Any> ApplicationCall.respondIdempotent(
    status: HttpStatusCode,
    message: T,
) {
    val requestId = attributes.getOrNull(RequestIdKey)
    val bodyHash = attributes.getOrNull(BodyHashKey)
    if (requestId != null && bodyHash != null && status.value in 200..299) {
        // Only cache successful responses (2xx)
        // 4xx errors (client mistakes) should be retried with corrected data
        // 5xx errors (server issues) should be retried after resolution
        val json = Json.encodeToString(message)

        // Store in idempotency store
        val koin =
            org.koin.java.KoinJavaComponent
                .getKoin()
        val cacher =
            koin.getOrNull(ResponseCacher::class)
                ?: DefaultResponseCacher(koin.getOrNull(IdempotencyStore::class)!!)
        val contentType = "application/json; charset=UTF-8"

        logger.info { "Storing idempotent response for requestId: $requestId, status: ${status.value}" }
        cacher.store(requestId, status.value, json, contentType, bodyHash)
    } else if (requestId != null) {
        logger.info { "Not caching error response for requestId: $requestId, status: ${status.value}" }
    }

    // Send the normal response
    respond(status, message)
}

/**
 * Extension to respond with status only (no body) with automatic idempotency caching.
 * Only caches successful responses (2xx) - errors should not be cached.
 */
suspend fun ApplicationCall.respondIdempotent(status: HttpStatusCode) {
    val requestId = attributes.getOrNull(RequestIdKey)
    val bodyHash = attributes.getOrNull(BodyHashKey)
    if (requestId != null && bodyHash != null && status.value in 200..299) {
        // Only cache successful responses (2xx)
        val koin =
            org.koin.java.KoinJavaComponent
                .getKoin()
        val cacher =
            koin.getOrNull(ResponseCacher::class)
                ?: DefaultResponseCacher(koin.getOrNull(IdempotencyStore::class)!!)
        val contentType = "application/json; charset=UTF-8"

        logger.info { "Storing idempotent no-body response for requestId: $requestId, status: ${status.value}" }
        cacher.store(requestId, status.value, "", contentType, bodyHash)
    } else if (requestId != null) {
        logger.info { "Not caching error response for requestId: $requestId, status: ${status.value}" }
    }

    // Send the normal response
    respond(status)
}

/**
 * Extension to receive request body.
 * With DoubleReceive plugin, we can call receiveText() multiple times.
 * This manually deserializes to match the extractRequestId() approach.
 */
suspend inline fun <reified T : Any> ApplicationCall.receiveIdempotent(): T {
    val bodyText = receiveText()
    return Json.decodeFromString(bodyText)
}
