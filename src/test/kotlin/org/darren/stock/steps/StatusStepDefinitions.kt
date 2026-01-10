package org.darren.stock.steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import kotlinx.coroutines.test.runTest
import net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals
import org.darren.stock.steps.helpers.removeAsciiDocs
import org.junit.jupiter.api.Assertions.assertEquals
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StatusStepDefinitions : KoinComponent {
    private lateinit var response: HttpResponse
    private val client: HttpClient by inject()
    private val serviceHelper: ServiceLifecycleSteps by inject()

    @When("I send a GET request to the {string} endpoint")
    fun iSendAGETRequestToTheEndpoint(endpoint: String) =
        runTest {
            response = client.request(endpoint)
        }

    @And("the response status code should be {int}")
    fun theResponseStatusCodeShouldBe(expectedStatusCode: Int) {
        assertEquals(
            expectedStatusCode,
            response.status.value,
            "Expected status code does not match the actual status code",
        )
    }

    @And("the response body should match JSON:")
    fun theResponseBodyShouldMatchJson(expectedJson: String) =
        runTest {
            val actualBody: String = response.body()
            val normalizedExpected = expectedJson.removeAsciiDocs().trimIndent()

            println("HEALTH_PROBE_RESPONSE_BODY=$actualBody")
            assertJsonEquals(normalizedExpected, actualBody)
        }

    @Given("the Location API health check will fail")
    fun theLocationApiHealthCheckWillFail() =
        runTest {
            serviceHelper.healthResponder = { it.respond(HttpStatusCode.ServiceUnavailable) }
        }
}
