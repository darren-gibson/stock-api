package com.darren.stock.steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator

class ApiCallStepDefinitions : KoinComponent {
    private lateinit var response: HttpResponse
    private val testApp: TestApplication by inject()
    private val client = testApp.client

    @When("I send a POST request to {string} with the following payload:")
    fun iSendAPOSTRequestToWithTheFollowingPayload(url: String, payload: String) = runBlocking {
        response = client.post(url) {
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

        JSONAssert.assertEquals(
            expectedResult, actualBody, CustomComparator(
                JSONCompareMode.LENIENT,
                Customization("createdAt", { o1, o2 -> true })
            )
        )
    }
}