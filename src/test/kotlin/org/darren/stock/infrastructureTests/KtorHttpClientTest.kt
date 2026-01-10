package org.darren.stock.infrastructureTests

import arrow.resilience.CircuitBreaker
import arrow.resilience.ktor.client.HttpCircuitBreaker
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class KtorHttpClientTest {
    private val failOnServerError =
        createClientPlugin("FailOnServerError") {
            onResponse { response ->
                val status = response.status.value
                // Treat commonly retryable server errors as failures so CircuitBreaker counts them.
                if (status == 500 || status == 502 || status == 503 || status == 504) {
                    throw ServerResponseException(response, "Server error: ${response.status}")
                }
            }
        }

    @Test
    fun `calling via the HttpRequestRetry retries the defined number of times`() =
        runTest {
            var numberOfCalls = 0

            val mockEngine =
                MockEngine { _ ->
                    ++numberOfCalls
                    respondError(HttpStatusCode.ServiceUnavailable)
                }
            val client =
                HttpClient(mockEngine) {
                    install(HttpRequestRetry) {
                        maxRetries = 1
                        delayMillis { _ -> 0 } // will retry in 3, 6, 9, etc. seconds
                    }
                }
            val response = client.get("/test")

            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals(2, numberOfCalls)
        }

    @Test
    fun `calling via the Circuit Breaker trips after the defined number of times with network errors`() =
        runTest {
            val expectedNumberOfCalls = 2
            var actualNumberOfCalls = 0

            val mockEngine =
                MockEngine { _ ->
                    ++actualNumberOfCalls
                    // Simulate network/connection failure - this WILL trip the circuit breaker
                    throw java.net.ConnectException("Connection refused")
                }

            val client =
                HttpClient(mockEngine) {
                    install(HttpCircuitBreaker) {
                        circuitBreaker(
                            resetTimeout = 15.seconds,
                            maxFailures = expectedNumberOfCalls - 1,
                        )
                    }
                }

            val responses =
                (0..expectedNumberOfCalls + 1).map {
                    runCatching { client.get("/test") }
                }

            assertEquals(expectedNumberOfCalls, actualNumberOfCalls)

            // Verify the first two failed with ConnectException, last two with ExecutionRejected
            assertIs<java.net.ConnectException>(responses[0].exceptionOrNull())
            assertIs<java.net.ConnectException>(responses[1].exceptionOrNull())
            assertIs<CircuitBreaker.ExecutionRejected>(responses[2].exceptionOrNull())
            assertIs<CircuitBreaker.ExecutionRejected>(responses[3].exceptionOrNull())
        }

    @Test
    fun `HTTP server errors can trip the Circuit Breaker when mapped to exceptions in the send pipeline`() =
        runTest {
            val expectedNumberOfCalls = 2
            var actualNumberOfCalls = 0

            val mockEngine =
                MockEngine { _ ->
                    ++actualNumberOfCalls
                    respondError(HttpStatusCode.InternalServerError)
                }

            val client =
                HttpClient(mockEngine) {
                    install(HttpCircuitBreaker) {
                        circuitBreaker(
                            resetTimeout = 15.seconds,
                            maxFailures = expectedNumberOfCalls - 1,
                        )
                    }

                    install(failOnServerError)
                }

            val responses =
                (0..expectedNumberOfCalls + 1).map {
                    runCatching { client.get("/test") }
                }

            assertEquals(expectedNumberOfCalls, actualNumberOfCalls)

            // First two calls fail with server-side errors mapped to exceptions; subsequent calls are short-circuited.
            assertIs<ServerResponseException>(responses[0].exceptionOrNull())
            assertIs<ServerResponseException>(responses[1].exceptionOrNull())
            assertIs<CircuitBreaker.ExecutionRejected>(responses[2].exceptionOrNull())
            assertIs<CircuitBreaker.ExecutionRejected>(responses[3].exceptionOrNull())
        }

    @Test
    fun `Circuit Breaker and Retry work together - circuit trips after counting retried failures`() =
        runTest {
            var actualNumberOfCalls = 0

            val mockEngine =
                MockEngine { _ ->
                    ++actualNumberOfCalls
                    respondError(HttpStatusCode.InternalServerError)
                }

            val client =
                HttpClient(mockEngine) {
                    // Retry logic - but don't retry if circuit is open
                    install(HttpRequestRetry) {
                        maxRetries = 2 // Each logical call attempts 3 times (1 original + 2 retries)
                    }

                    // CircuitBreaker wraps each individual attempt (including retries)
                    install(HttpCircuitBreaker) {
                        circuitBreaker(
                            resetTimeout = 15.seconds,
                            maxFailures = 4, // Opens after 5 backend call failures
                        )
                    }

                    install(failOnServerError)
                }

            // Make 3 logical requests
            val responses =
                (0..2).map {
                    runCatching { client.get("/test") }
                }

            // With CircuitBreaker counting each backend attempt:
            // Logical call 1: attempts 3 times = 3 backend calls (failures 1-3)
            // Logical call 2: attempts 2 times, circuit opens on 5th failure, no more retries = 2 backend calls
            // Logical call 3: circuit is open, immediate fail-fast = 0 backend calls
            assertEquals(5, actualNumberOfCalls)

            // First logical request: all retries exhausted
            assertIs<ServerResponseException>(responses[0].exceptionOrNull())

            // Second logical request: retries started but circuit opened during attempts
            assertIs<CircuitBreaker.ExecutionRejected>(responses[1].exceptionOrNull())

            // Third logical request: circuit is open, immediate fail-fast
            assertIs<CircuitBreaker.ExecutionRejected>(responses[2].exceptionOrNull())
        }

    @Test
    @Disabled("This test does not work because HTTP Errors do not trip the circuit breaker")
    fun `HTTP server errors can trip the Circuit Breaker`() =
        runTest {
            val expectedNumberOfCalls = 2
            var actualNumberOfCalls = 0

            val mockEngine =
                MockEngine { _ ->
                    ++actualNumberOfCalls
                    respondError(HttpStatusCode.InternalServerError)
                }

            val client =
                HttpClient(mockEngine) {
                    install(HttpCircuitBreaker) {
                        circuitBreaker(
                            resetTimeout = 15.seconds,
                            maxFailures = expectedNumberOfCalls - 1,
                        )
                    }
                }

            (0..expectedNumberOfCalls).forEach { _ ->
                client.get("/test")
            }
            assertEquals(expectedNumberOfCalls, actualNumberOfCalls)
        }
}
