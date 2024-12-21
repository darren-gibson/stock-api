package org.darren.stock.steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import net.javacrumbs.jsonunit.JsonMatchers.jsonEquals
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.dsl.module

class ApiCallStepDefinitions : KoinComponent {
    private lateinit var response: HttpResponse
    private val testApp: TestApplication by inject()
    private val client = testApp.client

    init {
        loadKoinModules(module { single { this@ApiCallStepDefinitions } })
    }

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

        assertThat(actualBody, jsonEquals(expectedResult.removeAsciiDocs()))
    }
}