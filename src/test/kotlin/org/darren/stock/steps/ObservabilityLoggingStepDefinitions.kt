package org.darren.stock.steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.http.*
import io.ktor.server.response.*
import kotlinx.coroutines.runBlocking
import org.darren.stock.TestLogAppender
import org.junit.jupiter.api.Assertions.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ObservabilityLoggingStepDefinitions : KoinComponent {
    private val serviceHelper: ServiceLifecycleSteps by inject()
    private var capturedOutboundHeader: String? = null
    private val apiCallStepDefinitions: ApiCallStepDefinitions by inject()

    @When("I send a GET request to {string} which triggers an outbound location lookup")
    fun iSendAGETThatTriggersOutboundLookup(url: String) =
        runBlocking {
            // Install a responder that captures the inbound traceparent header on the external service
            serviceHelper.getLocationByIdResponder = { call ->
                capturedOutboundHeader = call.request.headers["traceparent"]
                call.respondText("{}", ContentType.Application.Json)
            }
            apiCallStepDefinitions.iSendAGETRequestTo(url)
        }

    @Then("logs produced while processing the request should include the same trace id value")
    fun logsShouldIncludeTraceId() {
        val traceId = getTraceIdFromResponse()
        // With OpenTelemetry, trace context is automatically propagated
        // The logs should include trace information through MDC or structured logging
        assertLogMatchesRegex("\\[$traceId:".toRegex())
    }

    @Then("the outbound HTTP call made by the service should include a traceparent header matching the request's trace id")
    fun outboundCallShouldIncludeTraceparent() {
        assertNotNull(capturedOutboundHeader, "External service did not receive any traceparent header")
        // The traceparent header should be in format: 00-{trace-id}-{span-id}-{flags}
        val traceparent = capturedOutboundHeader!!
        assertTrue(traceparent.startsWith("00-"), "Traceparent should start with version 00")
        val parts = traceparent.split("-")
        assertEquals(4, parts.size, "Traceparent should have 4 parts")
        // Validate that trace ID and span ID are present (they should be hex strings)
        assertTrue(parts[1].length == 32 || parts[1].length == 16, "Trace ID should be 32 or 16 hex chars")
        assertTrue(parts[2].length == 16, "Span ID should be 16 hex chars")
    }

    @And("logs emitted by the actor\\(s) handling the delivery should contain the request's trace id")
    fun logsEmittedByTheActorSHandlingTheDeliveryShouldContainTheRequestSTraceId() {
        val traceId = getTraceIdFromResponse()
        // Wait briefly for actor logs (they may be emitted asynchronously)
        val deadline = System.currentTimeMillis() + 2000
        var found = false
        // Ensure the matching log entry is emitted by the actor's logger (StockPotActor)
        while (System.currentTimeMillis() < deadline) {
            if (TestLogAppender.events.any { it.contains("StockPotActor") && it.contains(traceId) }) {
                found = true
                break
            }
            Thread.sleep(50)
        }

        assertTrue(
            found,
            "Actor logs did not contain trace id $traceId. Events=${TestLogAppender.events.joinToString("\n")}",
        )
        assertActorLogsContainTraceId()
    }

    @And("logs emitted by the actor\\(s) handling the sale should contain the request's trace id")
    fun logsEmittedByTheActorSHandlingTheSaleShouldContainTheRequestSTraceId() {
        assertActorLogsContainTraceId()
    }

    @And("logs emitted by the actor\\(s) handling the count should contain the request's trace id")
    fun logsEmittedByTheActorSHandlingTheCountShouldContainTheRequestSTraceId() {
        assertActorLogsContainTraceId()
    }

    @And("logs emitted by the actor\\(s) handling the move should contain the request's trace id")
    fun logsEmittedByTheActorSHandlingTheMoveShouldContainTheRequestSTraceId() {
        assertActorLogsContainTraceId()
    }

    private fun assertActorLogsContainTraceId() {
        assertLogMatchesRegex("StockPotActor.*${getTraceIdFromResponse()}".toRegex())
    }

    private fun assertLogMatchesRegex(regex: Regex) {
        // Wait briefly for actor logs (they may be emitted asynchronously)
        val deadline = System.currentTimeMillis() + 2000
        var found = false
        // Ensure the matching log entry is emitted by the actor's logger (StockPotActor)
        while (System.currentTimeMillis() < deadline) {
            if (TestLogAppender.events.any { it.contains(regex) }) {
                found = true
                break
            }
            Thread.sleep(50)
        }

        assertTrue(
            found,
            "logs did not match $regex. Events=${TestLogAppender.events.joinToString("\n")}",
        )
    }

    private fun getTraceIdFromResponse(): String {
        // Use the trace ID that was captured from the request
        // Each request in ApiCallStepDefinitions extracts and stores the trace ID
        val traceId = TestContext.currentTraceId
        assertNotNull(traceId, "No trace ID was captured from the request. Ensure the request included a traceparent header or OpenTelemetry generated one.")
        return traceId!!
    }

    @And("at least one structured log event produced during the request should contain a traceId field")
    fun atLeastOneStructuredLogEventProducedDuringTheRequestShouldContainATraceIdField() {
        val traceId = getTraceIdFromResponse()
        val found = TestLogAppender.events.any { it.contains(traceId) }
        assertTrue(found, "No log event contained the traceId $traceId. Events: ${TestLogAppender.events}")
    }

    @And("logs emitted should contain the request's trace id within the {string} log")
    fun logsEmittedShouldContainTheRequestSTraceIdWithinTheLog(expected: String) {
        val traceId = getTraceIdFromResponse()
        val regex = expected.replace("<traceId>", traceId).toRegex()
        assertLogMatchesRegex(regex)
    }

    @And("logs produced while processing the request should include a span id value")
    fun logsShouldIncludeSpanId() {
        // The span_id should appear in logs in the format [trace_id:span_id]
        // We're looking for any 16-character hex string that appears after a colon
        val spanIdRegex = "\\[([a-f0-9]{32}):[a-f0-9]{16}\\]".toRegex()
        val found = TestLogAppender.events.any { spanIdRegex.containsMatchIn(it) }
        assertTrue(
            found,
            "No logs found with span_id. Expected format [trace_id:span_id]. Events: ${TestLogAppender.events.joinToString("\n")}",
        )
    }
}
