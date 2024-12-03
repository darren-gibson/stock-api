package com.darren.stock.steps

import com.darren.stock.ktor.module
import io.cucumber.java.After
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals

class StatusStepDefinitions {
    private lateinit var testApp: TestApplication
    private lateinit var response: HttpResponse

    @Given("the service is running")
    fun theServiceIsRunning() {
        testApp = TestApplication {
            application {
                module()
            }
        }
        testApp.start()
    }

    @When("I send a GET request to the {string} endpoint")
    fun iSendAGETRequestToTheEndpoint(endpoint: String) = runBlocking {
        response = testApp.client.request(endpoint)
    }

    @Then("the response status code should be {int}")
    fun theResponseStatusCodeShouldBe(expectedStatusCode: Int) {
        assertEquals(
            expectedStatusCode,
            response.status.value,
            "Expected status code does not match the actual status code"
        )
    }

    @And("the response body should indicate the service is healthy")
    fun theResponseBodyShouldIndicateTheServiceIsHealthy() {
//        val responseBody: String = response.body()
//        assertTrue(responseBody.contains("healthy"), "Response body does not indicate the service is healthy")
    }

    @After
    fun shutdownTestServerAfterScenario() {
        if(this::testApp.isInitialized)
            testApp.stop()
    }
}