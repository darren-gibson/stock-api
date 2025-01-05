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
import org.darren.stock.steps.helpers.removeAsciiDocs
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.test.assertContains

class ApiCallStepDefinitions : KoinComponent {
    private lateinit var response: HttpResponse
    private val client: HttpClient by inject()

    @When("I send a POST request to {string} with the following payload:")
    fun iSendAPOSTRequestToWithTheFollowingPayload(url: String, payload: String) = runBlocking {
        response = sendPostRequest(url, payload.removeAsciiDocs())
    }

    suspend fun sendPostRequest(url: String, payload: String): HttpResponse {
        return client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payload)
            //            headers {
            //                append(HttpHeaders.Authorization, "Bearer YOUR_ACCESS_TOKEN")
            //            }
        }
    }

    @Then("the API should respond with status code {int}")
    fun theAPIShouldRespondWithStatusCode(expectedStatusCode: Int) {
        assertEquals(expectedStatusCode, response.status.value)
    }

    @When("I send a GET request to {string}")
    fun iSendAGETRequestTo(url: String): HttpResponse = runBlocking {
        response = client.get(url)
        return@runBlocking response
    }

    @When("I send a PUT request to {string} with the following payload:")
    fun iSendAPUTRequestToWithTheFollowingPayload(url: String, payload: String) = runBlocking {
        response = sendPutRequest(url, payload.removeAsciiDocs())
    }

    private suspend fun sendPutRequest(url: String, payload: String): HttpResponse {
        return client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payload)
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
    fun theResponseBodyShouldContain(expectedResult: String) = runBlocking {
        val actualBody = response.bodyAsText()

        assertThat(actualBody, jsonEquals(expectedResult.removeAsciiDocs().ignoreTimestamps()))
    }

    private fun String.ignoreTimestamps(): String {
        return this.replace("<timestamp>", "\${json-unit.ignore}")
    }
}
