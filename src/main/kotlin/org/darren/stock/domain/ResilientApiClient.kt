package org.darren.stock.domain

import arrow.resilience.CircuitBreaker
import arrow.resilience.ktor.client.HttpCircuitBreaker
import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.instrumentation.ktor.v3_0.KtorClientTelemetry
import kotlinx.serialization.json.Json
import org.darren.stock.domain.resilience.ApiResilienceConfig
import org.darren.stock.util.wrapHttpCallWithLogging
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

/**
 * Generic HTTP client with built-in resilience (retry + circuit breaker).
 * Handles common HTTP error patterns and exception mapping for downstream services.
 *
 * Subclasses should override handleResponseException() to provide domain-specific error handling.
 *
 * ## Error Handling & Retries
 *
 * All transient failures are retried consistently:
 * - **5xx Server Errors** (500, 502, 503, 504): Converted to ServerResponseException and retried
 * - **Connection Errors** (timeout, refused, network unreachable): Retried by HttpRequestRetry plugin
 * - **Read/Write Timeouts**: Retried with exponential backoff (configurable via resilience.backoff parameters)
 *
 * After retries are exhausted:
 * - 5xx errors and connection failures both map to HTTP 502 Service Unavailable
 * - This ensures consistent error handling regardless of failure type
 *
 * ## Observability
 *
 * Circuit breaker state changes can be observed through:
 * - **Exceptions**: `CircuitBreaker.ExecutionRejected` thrown when circuit is open (converted to 502 by handlers)
 * - **Metrics**: Arrow's circuit breaker emits metrics compatible with Micrometer/OpenTelemetry
 * - **Logging**: Enable DEBUG logging for `arrow.resilience` to see state transitions:
 *   - Circuit opening: `CircuitBreaker transitioned from Closed to Open`
 *   - Half-open attempts: `CircuitBreaker transitioned from Open to HalfOpen`
 *   - Circuit recovery: `CircuitBreaker transitioned from HalfOpen to Closed`
 *
 * Configure in logback.xml:
 * ```xml
 * <logger name="arrow.resilience" level="DEBUG"/>
 * ```
 */
abstract class ResilientApiClient(
    protected val engine: HttpClientEngine,
) {
    protected lateinit var logger: KLogger
    protected lateinit var client: HttpClient

    /**
     * Build the HTTP client with resilience plugins (retry + circuit breaker).
     * Subclasses can override to customize client configuration.
     *
     * ## Plugin Ordering
     * Plugins are installed in a specific order for correct behavior:
     * 1. HttpRequestRetry - Wraps individual HTTP calls with exponential backoff
     * 2. HttpCircuitBreaker - Wraps retry attempts, counting each backend call as pass/fail
     * 3. ThrowOn5xx - Converts 5xx responses to exceptions so circuit breaker counts them
     *
     * This ordering ensures the circuit breaker sees individual backend failures rather than
     * just the final retry outcome.
     */
    protected open fun buildClient(config: ApiResilienceConfig): HttpClient {
        val throwOn5xx =
            createClientPlugin("ThrowOn5xx") {
                onResponse { response ->
                    // Throw exceptions on retryable 5xx server errors so CircuitBreaker counts them
                    if (response.status.value in listOf(500, 502, 503, 504)) {
                        throw ServerResponseException(response, "Server error: ${response.status}")
                    }
                }
            }

        return HttpClient(engine) {
            // Install Retry plugin first so CircuitBreaker wraps each attempt
            install(HttpRequestRetry) {
                maxRetries = maxOf(0, config.backoff.maxAttempts - 1)
                delayMillis { retryCount ->
                    val exponentialDelay =
                        config.backoff.initialDelay.inWholeMilliseconds
                            .toDouble() *
                            config.backoff.multiplier.pow(retryCount.toDouble())
                    minOf(exponentialDelay.toLong(), config.backoff.maxDelay.inWholeMilliseconds)
                }
                retryOnException(retryOnTimeout = true)
            }

            // CircuitBreaker wraps each retry attempt so it counts individual backend calls
            install(HttpCircuitBreaker) {
                circuitBreaker(
                    resetTimeout = config.failFast.quietPeriod,
                    maxFailures = config.failFast.failureThreshold - 1,
                )
            }

            // Throw exceptions on 5xx server errors so CircuitBreaker counts them as failures
            install(throwOn5xx)
            install(KtorClientTelemetry) { setOpenTelemetry(GlobalOpenTelemetry.get()) }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        coerceInputValues = true
                        explicitNulls = false
                    },
                )
            }
            install(HttpCache)
            install(HttpTimeout) {
                connectTimeoutMillis = 1.seconds.inWholeMilliseconds
                requestTimeoutMillis = 2.seconds.inWholeMilliseconds
                socketTimeoutMillis = requestTimeoutMillis
            }
        }
    }

    /**
     * Get the resilience configuration for this client.
     * Subclasses must implement to provide their specific configuration.
     */
    protected abstract fun getResilienceConfig(): ApiResilienceConfig

    /**
     * Update the HTTP client when resilience configuration changes.
     */
    protected fun updateClient(config: ApiResilienceConfig) {
        val newClient = buildClient(config)
        val oldClient =
            synchronized(this) {
                val previous = client
                client = newClient
                previous
            }
        oldClient.close()
    }

    /**
     * Execute a generic HTTP GET request with automatic response handling and error mapping.
     * Handles common HTTP errors (4xx/5xx) and maps them to domain exceptions via handleResponseException().
     * Connection errors are retried by HttpRequestRetry plugin; if exhausted, they're treated as service unavailable.
     *
     * @param T The expected response type
     * @param url The full URL to request
     * @param context Additional context for error handling (e.g., resource identifier)
     * @return The deserialized response body
     * @throws Exception Domain-specific exceptions from handleResponseException()
     */
    protected suspend inline fun <reified T> executeRequest(
        url: String,
        context: String = "",
    ): T =
        try {
            val response = wrapHttpCallWithLogging(logger) { client.get(url) }
            if (response.status.isSuccess()) {
                response.body()
            } else {
                handleResponseException(response.status, context)
            }
        } catch (e: CircuitBreaker.ExecutionRejected) {
            // Circuit breaker is open - treat as upstream unavailable
            logger.warn { "Circuit breaker opened: failing fast to protect upstream service" }
            handleResponseException(HttpStatusCode.ServiceUnavailable, context)
        } catch (e: ServerResponseException) {
            // ThrowOn5xx plugin throws ServerResponseException on retryable 5xx errors
            logger.warn { "Server error (${e.response.status}) after retries exhausted" }
            handleResponseException(e.response.status, context)
        } catch (e: ResponseException) {
            if (e is ClientRequestException) {
                handleResponseException(e.response.status, context)
            } else {
                throw e
            }
        } catch (e: java.io.IOException) {
            // Connection errors (timeout, connection refused, network unreachable, etc.)
            // after retries exhausted - treat as service unavailable
            logger.warn { "Connection error after retries exhausted: ${e.message}" }
            handleResponseException(HttpStatusCode.ServiceUnavailable, context)
        }

    /**
     * Handle HTTP response errors and map them to domain-specific exceptions.
     * Subclasses should override to provide their own error mapping logic.
     *
     * @param status The HTTP status code
     * @param context Additional context for error messages
     * @throws Exception Domain-specific exception
     */
    protected open fun handleResponseException(
        status: HttpStatusCode,
        context: String,
    ): Nothing = throw Exception("HTTP error: $status")
}
