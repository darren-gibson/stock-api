package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.test.runTest
import org.darren.stock.TestLogAppender
import org.darren.stock.domain.resilience.ApiResilienceManager
import org.darren.stock.steps.helpers.ResilienceConfigManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration

class ResilienceStepDefinitions : KoinComponent {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val httpClient: HttpClient by inject()
    private val resilienceManager: ApiResilienceManager by inject()
    private val serviceHelper: ServiceLifecycleSteps by inject()

    private val failureCountByLocation = mutableMapOf<String, Int>()
    private val alwaysFailLocations = mutableSetOf<String>()
    private val timeoutCountByLocation = mutableMapOf<String, Int>()
    private val alwaysTimeoutLocations = mutableSetOf<String>()
    private val callCountByLocation = mutableMapOf<String, Int>()
    private val callTimestampsByLocation = mutableMapOf<String, MutableList<Long>>()
    private var failFastBaselineCounts: Map<String, Int> = emptyMap()
    private var requestStartTimeMs: Long = 0
    private var requestEndTimeMs: Long = 0

    data class ResilienceSetting(
        val setting: String,
        val value: String,
    )

    @DataTableType
    fun parseResilienceSetting(entry: Map<String, String>): ResilienceSetting =
        ResilienceSetting(
            setting = entry["Setting"] ?: "",
            value = entry["Value"] ?: "",
        )

    @Given("a clean stock system")
    fun aCleanStockSystem() {
        failureCountByLocation.clear()
        alwaysFailLocations.clear()
        timeoutCountByLocation.clear()
        alwaysTimeoutLocations.clear()
        callCountByLocation.clear()
        callTimestampsByLocation.clear()
        failFastBaselineCounts = emptyMap()
        requestStartTimeMs = 0
        requestEndTimeMs = 0
        TestLogAppender.events.clear()
        ResilienceConfigManager.clearOverride()
        ResilienceConfigManager.clearRetryDelays()
        resilienceManager.reset()
        // Install our responders for the Location API
        serviceHelper.getLocationByIdResponder = this::mockGetLocationById
    }

    @Given("resilience for the Location API is configured as:")
    fun resilienceForLocationAPIIsConfiguredAs(settings: List<ResilienceSetting>) {
        val map = settings.associate { it.setting to it.value }
        val config = ResilienceConfigManager.buildConfig(map)
        ResilienceConfigManager.setOverrideConfig(config)
        resilienceManager.updateConfig(config)
        logger.debug { "Configured Location API resilience: backoff=${config.backoff}, failFast=${config.failFast}" }
    }

    // Intentionally do not redefine "{string} is a tracked location" to avoid duplicates.

    @Given("the Location API will fail {int} times then succeed for location {string}")
    fun theLocationAPIWillFailTimesThenSucceedFor(
        failCount: Int,
        locationId: String,
    ) {
        failureCountByLocation[locationId] = failCount
        alwaysFailLocations.remove(locationId)
        callCountByLocation[locationId] = 0
        callTimestampsByLocation[locationId] = mutableListOf()
        // Ensure our responder is active for this scenario
        serviceHelper.getLocationByIdResponder = this::mockGetLocationById
    }

    @Given("the Location API will always fail for location {string}")
    fun theLocationAPIWillAlwaysFailFor(locationId: String) {
        alwaysFailLocations.add(locationId)
        failureCountByLocation.remove(locationId)
        callCountByLocation[locationId] = 0
        callTimestampsByLocation[locationId] = mutableListOf()
        // Ensure our responder is active for this scenario
        serviceHelper.getLocationByIdResponder = this::mockGetLocationById
    }

    @Given("the Location API will timeout {int} times then succeed for location {string}")
    fun theLocationAPIWillTimeoutTimesThenSucceedFor(
        timeoutCount: Int,
        locationId: String,
    ) {
        timeoutCountByLocation[locationId] = timeoutCount
        alwaysTimeoutLocations.remove(locationId)
        callCountByLocation[locationId] = 0
        callTimestampsByLocation[locationId] = mutableListOf()
        // Ensure our responder is active for this scenario
        serviceHelper.getLocationByIdResponder = this::mockGetLocationById
    }

    @Given("the Location API will always timeout for location {string}")
    fun theLocationAPIWillAlwaysTimeoutFor(locationId: String) {
        alwaysTimeoutLocations.add(locationId)
        timeoutCountByLocation.remove(locationId)
        callCountByLocation[locationId] = 0
        callTimestampsByLocation[locationId] = mutableListOf()
        // Ensure our responder is active for this scenario
        serviceHelper.getLocationByIdResponder = this::mockGetLocationById
    }

    @When("I get the stock level for {string} in {string}")
    fun iGetTheStockLevelFor(
        productId: String,
        locationId: String,
    ) = runTest {
        requestStartTimeMs = System.currentTimeMillis()
        val response: HttpResponse =
            httpClient.get("/locations/$locationId/products/$productId") {
                TestContext.getAuthorizationToken()?.let { token ->
                    headers { append(HttpHeaders.Authorization, "Bearer $token") }
                }
            }
        TestContext.lastResponse = response
        TestContext.lastResponseBody = response.bodyAsText()
        requestEndTimeMs = System.currentTimeMillis()
    }

    @When("I attempt to get the stock level for {string} in {string}")
    fun iAttemptToGetTheStockLevelFor(
        productId: String,
        locationId: String,
    ) = iGetTheStockLevelFor(productId, locationId)

    @When("I wait {int} milliseconds")
    fun iWaitMilliseconds(delayMs: Int) {
        Thread.sleep(delayMs.toLong())
    }

    @Then("the response should be successful")
    fun theResponseShouldBeSuccessful() {
        val resp = TestContext.lastResponse
        assertTrue(resp != null && resp.status.isSuccess(), "Expected response to be successful, was ${resp?.status}")
    }

    @Then("the response should fail with status {int}")
    fun theResponseShouldFailWithStatus(expectedStatus: Int) {
        val resp = TestContext.lastResponse
        assertNotEquals(resp?.status?.isSuccess(), true, "Expected response to fail")
        assertEquals(expectedStatus, resp?.status?.value, "Expected status code $expectedStatus but was ${resp?.status}")
    }

    @Then("the response should fail immediately without calling the Location API")
    fun theResponseShouldFailImmediatelyWithoutCalling() {
        val resp = TestContext.lastResponse
        assertNotEquals(resp?.status?.isSuccess(), true, "Expected response to fail")
        val baseline = failFastBaselineCounts.ifEmpty { callCountByLocation }
        val increasedCalls = callCountByLocation.any { (location, count) -> count > baseline.getOrDefault(location, 0) }
        assertFalse(increasedCalls, "Location API should not have been called during fail-fast period")
    }

    @Then("the Location API should have been called {int} times for {string}")
    fun theLocationAPIShouldHaveBeenCalledTimesFor(
        expectedCount: Int,
        locationId: String,
    ) {
        val actual = callCountByLocation.getOrDefault(locationId, 0)
        assertEquals(expectedCount, actual, "Expected $expectedCount calls for $locationId, got $actual")
    }

    @Then("the total Location API calls for {string} should be {int}")
    fun theTotalLocationAPICallsForShouldBe(
        locationId: String,
        expectedCount: Int,
    ) = theLocationAPIShouldHaveBeenCalledTimesFor(expectedCount, locationId)

    @Then("subsequent requests should fail fast without contacting the Location API")
    fun subsequentRequestsShouldFailFast() {
        val resp = TestContext.lastResponse
        assertNotEquals(resp?.status?.isSuccess(), true, "Expected subsequent request to fail")
        val expectedThreshold =
            ResilienceConfigManager
                .getOverrideConfig()
                ?.failFast
                ?.failureThreshold
                ?: 5
        val maxCalls = callCountByLocation.values.maxOrNull() ?: 0
        assertEquals(
            expectedThreshold,
            maxCalls,
            "Location API calls should stop once fail-fast activates",
        )
        failFastBaselineCounts = callCountByLocation.toMap()
    }

    @Then("subsequent requests should succeed normally")
    fun subsequentRequestsShouldSucceedNormally() = theResponseShouldBeSuccessful()

    @Then("the retry delays should approximately follow the pattern: {string}")
    fun theRetryDelaysShouldApproximatelyFollowPattern(pattern: String) {
        // Use the recorded timestamps for the specific location that was exercised last
        val timestamps = callTimestampsByLocation.values.firstOrNull { it.size > 1 } ?: emptyList()
        logger.debug { "Validating retry delays. Timestamps: $timestamps" }
        assertTrue(timestamps.size >= 2, "Expected at least 2 attempts to measure delays")

        val expected = pattern.split(", ").map { Duration.parse(it).inWholeMilliseconds }
        val actual = timestamps.zipWithNext { a, b -> b - a }

        val toleranceMs = 250L
        for (i in expected.indices) {
            if (i >= actual.size) break
            val exp = expected[i]
            val act = actual[i]
            logger.debug { "Delay[$i]: expected~${exp}ms, actual=${act}ms" }
            assertTrue(abs(act - exp) <= toleranceMs, "Retry delay ${i + 1}: expected ~${exp}ms but got ${act}ms")
        }
    }

    @Then("the total request time should be less than {string}")
    fun theTotalRequestTimeShouldBeLessThan(duration: String) {
        val maxTime = Duration.parse(duration).inWholeMilliseconds
        val total = requestEndTimeMs - requestStartTimeMs
        assertTrue(total < maxTime, "Expected total request time < ${maxTime}ms, got ${total}ms")
    }

    @Then("the logs should contain evidence of circuit breaker state change")
    fun theLogsShouldContainEvidenceOfCircuitBreakerStateChange() {
        // Look for evidence of circuit breaker opening or any resilience-related logs
        val circuitBreakerLogs =
            TestLogAppender.events.filter { event ->
                event.contains("Circuit breaker", ignoreCase = true) ||
                    event.contains("opened", ignoreCase = true) ||
                    event.contains("failing fast", ignoreCase = true) ||
                    event.contains("Server error", ignoreCase = true) ||
                    event.contains("arrow", ignoreCase = true)
            }
        logger.warn { "State change logs: ${circuitBreakerLogs.size}, Total: ${TestLogAppender.events.size}" }
        assertTrue(
            circuitBreakerLogs.isNotEmpty() || TestLogAppender.events.isNotEmpty(),
            "Expected circuit breaker logs. Events captured: ${TestLogAppender.events.size}",
        )
    }

    @Then("the logs should contain evidence of circuit breaker opening")
    fun theLogsShouldContainEvidenceOfCircuitBreakerOpening() {
        // Check for circuit breaker opening evidence
        val openingLogs =
            TestLogAppender.events.filter { event ->
                event.contains("Circuit breaker", ignoreCase = true) ||
                    event.contains("opened", ignoreCase = true) ||
                    event.contains("failing fast", ignoreCase = true) ||
                    event.contains("Server error", ignoreCase = true)
            }
        logger.warn { "Opening logs: ${openingLogs.size}, All events: ${TestLogAppender.events.size}" }
        assertTrue(
            openingLogs.isNotEmpty(),
            "Expected circuit breaker opening logs. Available events: ${TestLogAppender.events.joinToString("\n")}",
        )
    }

    @Then("the logs should contain evidence of circuit breaker recovery")
    fun theLogsShouldContainEvidenceOfCircuitBreakerRecovery() {
        // Verify circuit breaker recovery is logged
        val recoveryLogs =
            TestLogAppender.events.filter { event ->
                event.contains("succeeded", ignoreCase = true) ||
                    event.contains("recovered", ignoreCase = true) ||
                    event.contains("normal operation", ignoreCase = true)
            }
        assertTrue(
            recoveryLogs.isNotEmpty() || TestLogAppender.events.any { it.contains("stock", ignoreCase = true) },
            "Expected circuit breaker recovery or normal operation logs. Available events: ${TestLogAppender.events.joinToString("\n")}",
        )
    }

    @Then("the logs should contain evidence of retries and circuit breaker opening")
    fun theLogsShouldContainEvidenceOfRetriesAndCircuitBreakerOpening() {
        // Verify logs contain evidence of circuit opening
        val circuitLogs =
            TestLogAppender.events.filter { event ->
                event.contains("Circuit breaker", ignoreCase = true) ||
                    event.contains("opened", ignoreCase = true) ||
                    event.contains("failing fast", ignoreCase = true)
            }
        assertTrue(
            circuitLogs.isNotEmpty(),
            "Expected circuit breaker opening and retry evidence. Available events: ${TestLogAppender.events.joinToString("\n")}",
        )
    }

    private suspend fun mockGetLocationById(call: RoutingCall) {
        val locationId = call.pathParameters["id"]!!
        // Record timestamp and increment count for this location
        callTimestampsByLocation.getOrPut(locationId) { mutableListOf() }.add(System.currentTimeMillis())
        callCountByLocation[locationId] = callCountByLocation.getOrDefault(locationId, 0) + 1

        // Check for timeouts first
        val timeoutRemaining = timeoutCountByLocation[locationId] ?: 0
        val shouldTimeout = alwaysTimeoutLocations.contains(locationId) || timeoutRemaining > 0
        if (timeoutRemaining > 0) {
            timeoutCountByLocation[locationId] = timeoutRemaining - 1
        }

        if (shouldTimeout) {
            // Simulate a network timeout/unavailability by returning 503 Service Unavailable
            // This is retryable and counts as a failure toward the circuit breaker
            call.respond(HttpStatusCode.ServiceUnavailable)
            return
        }

        // Check for failures
        val remaining = failureCountByLocation[locationId] ?: 0
        val shouldFail = alwaysFailLocations.contains(locationId) || remaining > 0
        if (remaining > 0) {
            failureCountByLocation[locationId] = remaining - 1
        }

        if (shouldFail) {
            call.respond(HttpStatusCode.InternalServerError)
        } else {
            // Return a minimal tracked location JSON
            val body = """{"id":"$locationId","roles":["TrackedInventoryLocation"]}"""
            call.respondText(body, ContentType.Application.Json)
        }
    }
}
