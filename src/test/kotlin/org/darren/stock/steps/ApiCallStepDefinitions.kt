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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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

    @And("the response body should contain:")
    fun theResponseBodyShouldContain(expectedResult: String) = runBlocking {
        val actualBody = response.bodyAsText()

        assertThat(actualBody, jsonEquals(expectedResult.removeAsciiDocs().ignoreTimestamps()))
    }

    private fun String.ignoreTimestamps(): String {
        return this.replace("<timestamp>", "\${json-unit.ignore}")
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
}
