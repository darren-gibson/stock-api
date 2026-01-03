package org.darren.stock.steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import net.javacrumbs.jsonunit.JsonMatchers.jsonEquals
import org.darren.stock.TestLogAppender
import org.darren.stock.steps.helpers.removeAsciiDocs
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ApiCallStepDefinitions : KoinComponent {
    private lateinit var response: HttpResponse
    private val client: HttpClient by inject()

    // TODO: Apply DRY principle to reduce duplication

    @When("I send a POST request to {string} with the following payload:")
    @When("I send a POST request to {string} with the payload:")
    fun iSendAPOSTRequestToWithTheFollowingPayload(
        url: String,
        payload: String,
    ) = runBlocking {
        TestLogAppender.events.clear()
        val cleanPayload = payload.removeAsciiDocs()
        TestContext.lastRequestBody = cleanPayload
        response = sendPostRequest(url, cleanPayload)
        TestContext.lastResponse = response
        TestContext.lastResponseBody = response.bodyAsText()
        captureTraceIdFromLogs()
    }

    private fun captureTraceIdFromLogs() {
        // Extract trace ID from OpenTelemetry span logging
        val traceIdPattern = Regex("Trace from OpenTelemetry traceId=([0-9a-f]{32})")
        for (event in TestLogAppender.events) {
            val match = traceIdPattern.find(event)
            if (match != null) {
                TestContext.currentTraceId = match.groupValues[1]
                return
            }
        }
    }

    suspend fun sendPostRequest(
        url: String,
        payload: String,
    ): HttpResponse =
        client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payload)
            TestContext.getAuthorizationToken()?.let { token ->
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }

    @Then("the API should respond with status code {int}")
    fun theAPIShouldRespondWithStatusCode(expectedStatusCode: Int) {
        val response = if (this::response.isInitialized) response else TestContext.lastResponse
        assertNotNull(response, "No response available to verify status code")
        assertEquals(expectedStatusCode, response!!.status.value)
    }

    @When("I send a GET request to {string} without a {string} header")
    fun iSendAGETRequestToWithoutAXCorrelationIdHeader(
        url: String,
        headerToOmit: String,
    ) = runBlocking {
        response =
            client.get(url) {
                getStandardHeaders()
                headers.remove(headerToOmit)
            }
        TestContext.lastResponseBody = response.bodyAsText()
        captureTraceIdFromLogs()
        return@runBlocking response
    }

    private fun HttpRequestBuilder.getStandardHeaders() {
        TestContext.getAuthorizationToken()?.let { token ->
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    @When("I send a GET request to {string}")
    fun iSendAGETRequestTo(url: String): HttpResponse =
        runBlocking {
            response =
                client.get(url) {
                    getStandardHeaders()
                }
            TestContext.lastResponseBody = response.bodyAsText()
            TestContext.lastResponse = response
            captureTraceIdFromLogs()
            return@runBlocking response
        }

    @When("I send a GET request to {string} with header {string} -> {string}")
    fun iSendAGETRequestToWithHeaderXCorrelationIdMyCustomId(
        url: String,
        headerName: String,
        headerValue: String,
    ) {
        runBlocking {
            response =
                client.get(url) {
                    TestContext.getAuthorizationToken()?.let { token ->
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $token")
                            append(headerName, headerValue)
                        }
                    }
                }
            TestContext.lastResponse = response
            TestContext.lastResponseBody = response.bodyAsText()
            // For traceparent header, extract the trace ID directly from the header value
            if (headerName == "traceparent") {
                val parts = headerValue.split("-")
                if (parts.size >= 2) {
                    TestContext.currentTraceId = parts[1] // trace ID is the second part
                }
            } else {
                captureTraceIdFromLogs()
            }
        }
    }

    @When("I send a GET request to {string} without authentication")
    fun iSendAGETRequestToWithoutAuthentication(url: String): HttpResponse =
        runBlocking {
            response = client.get(url)
            TestContext.lastResponseBody = response.bodyAsText()
            return@runBlocking response
        }

    @When("I send a PUT request to {string} with the following payload:")
    fun iSendAPUTRequestToWithTheFollowingPayload(
        url: String,
        payload: String,
    ) = runBlocking {
        response = sendPutRequest(url, payload.removeAsciiDocs())
        TestContext.lastResponseBody = response.bodyAsText()
    }

    private suspend fun sendPutRequest(
        url: String,
        payload: String,
    ): HttpResponse =
        client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payload)
            TestContext.getAuthorizationToken()?.let { token ->
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }

    @And("the response headers should contain:")
    fun theResponseHeadersShouldContain(expectedHeaders: String) {
        expectedHeaders.split("\n").forEach { line ->
            val parts = line.split(":").map { it.trim() }
            val expectedHeader = parts[0]
            val expectedValue = parts[1]

            assertTrue(response.headers.contains(expectedHeader), "Missing expected header: $expectedHeader")
            val actualValue = response.headers[expectedHeader]
            assertEquals(expectedValue, actualValue)
        }
    }

    @And("the response body should contain:")
    fun theResponseBodyShouldContain(expectedResult: String) =
        runBlocking {
            val actualBody = response.bodyAsText()

            assertThat(actualBody, jsonEquals(expectedResult.removeAsciiDocs().ignoreTimestamps()))
        }

    @Then("the response header {string} should be present")
    fun responseHeaderShouldBePresent(headerName: String) {
        assertNotNull(response)
        assertTrue(response.headers.contains(headerName), "Response missing '$headerName' header")
    }

    @Then("the response header {string} should not be present")
    fun responseHeaderShouldNotBePresent(headerName: String) {
        val response = TestContext.lastResponse
        assertNotNull(response)
        val headerValue = response!!.headers[headerName]
        assertTrue(headerValue == null, "Response should not contain '$headerName' header but found: $headerValue")
    }

    @Then("the response header {string} should equal {string}")
    fun responseHeaderShouldEqual(
        header: String,
        expected: String,
    ) {
        val response = TestContext.lastResponse
        assertNotNull(response)
        assertEquals(expected, response!!.headers[header])
    }

    @Then("the response header {string} should be a valid traceparent format")
    fun responseHeaderShouldBeValidTraceparent(headerName: String) {
        assertNotNull(response)
        val header = response.headers[headerName]
        assertNotNull(header, "Missing '$headerName' header")

        // Traceparent format: 00-{trace-id}-{span-id}-{flags}
        val traceparentPattern = Regex("^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$")
        assertTrue(traceparentPattern.matches(header!!), "'$headerName' was not a valid traceparent format: $header")
    }

    @Then("the response header {string} should preserve trace id from {string} but have a different span id")
    fun responseHeaderShouldPreserveTraceId(
        headerName: String,
        originalTraceparent: String,
    ) {
        val response = TestContext.lastResponse
        assertNotNull(response)
        val responseTraceparent = response!!.headers[headerName]
        assertNotNull(responseTraceparent, "Missing '$headerName' header in response")

        // Parse original traceparent: 00-{trace-id}-{span-id}-{flags}
        val originalParts = originalTraceparent.split("-")
        require(originalParts.size == 4) { "Invalid original traceparent format" }
        val originalTraceId = originalParts[1]
        val originalSpanId = originalParts[2]

        // Parse response traceparent
        val responseParts = responseTraceparent!!.split("-")
        require(responseParts.size == 4) { "Invalid response traceparent format" }
        val responseTraceId = responseParts[1]
        val responseSpanId = responseParts[2]

        // Assert trace ID is preserved
        assertEquals(originalTraceId, responseTraceId, "Trace ID should be preserved from the incoming request")

        // Assert span ID is different (server created a new span)
        assertNotEquals(
            originalSpanId,
            responseSpanId,
            "Span ID should be different (server should create a new span for the request)",
        )
    }

    private fun String.ignoreTimestamps(): String = this.replace("<timestamp>", $$"${json-unit.ignore}")
}
