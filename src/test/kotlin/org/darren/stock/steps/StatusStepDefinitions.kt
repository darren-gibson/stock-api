package org.darren.stock.steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StatusStepDefinitions : KoinComponent {
    private lateinit var response: HttpResponse
    private val client: HttpClient by inject()

    @When("I send a GET request to the {string} endpoint")
    fun iSendAGETRequestToTheEndpoint(endpoint: String) =
        runTest {
            response = client.request(endpoint)
        }

    @Then("the response status code should be {int}")
    fun theResponseStatusCodeShouldBe(expectedStatusCode: Int) {
        assertEquals(
            expectedStatusCode,
            response.status.value,
            "Expected status code does not match the actual status code",
        )
    }

    @And("the response body should indicate the service is healthy")
    fun theResponseBodyShouldIndicateTheServiceIsHealthy() =
        runTest {
            val responseBody: String = response.body()
            assertTrue(responseBody.contains("Healthy"), "Response body does not indicate the service is healthy, body=$responseBody")
        }
}
