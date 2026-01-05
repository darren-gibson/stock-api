package org.darren.stock.steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals
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
            val bodyText: String = response.body()
            val payload = Json.parseToJsonElement(bodyText).jsonObject
            val status = payload["status"]?.jsonPrimitive?.content ?: ""
            assertEquals("Healthy", status, "Status field did not report Healthy: body=$bodyText")
        }

    @And("the response body should include version and build time metadata")
    fun theResponseBodyShouldIncludeVersionAndBuildTimeMetadata() =
        runTest {
            val bodyText: String = response.body()
            val payload = Json.parseToJsonElement(bodyText).jsonObject

            val version = payload["version"]?.jsonPrimitive?.content ?: ""
            val buildTime = payload["buildTime"]?.jsonPrimitive?.content ?: ""

            assertTrue(version.isNotBlank(), "Version must be present in status payload: body=$bodyText")
            assertTrue(buildTime.isNotBlank(), "Build time must be present in status payload: body=$bodyText")
        }

    @And("the response body should match the status contract")
    fun theResponseBodyShouldMatchTheStatusContract() =
        runTest {
            val bodyText: String = response.body()
            val payload = Json.parseToJsonElement(bodyText).jsonObject

            val expectedKeys = setOf("status", "version", "buildTime")
            assertEquals(expectedKeys, payload.keys, "Status payload keys mismatch: body=$bodyText")

            val status = payload["status"]?.jsonPrimitive?.content ?: ""
            val version = payload["version"]?.jsonPrimitive?.content ?: ""
            val buildTime = payload["buildTime"]?.jsonPrimitive?.content ?: ""

            assertEquals("Healthy", status, "Status field did not report Healthy: body=$bodyText")
            assertTrue(version.isNotBlank(), "Version must be present in status payload: body=$bodyText")
            assertTrue(buildTime.isNotBlank(), "Build time must be present in status payload: body=$bodyText")
        }

    @And("the response body should match JSON:")
    fun theResponseBodyShouldMatchJson(expectedJson: String) =
        runTest {
            val actualBody: String = response.body()
            val timestampMatcher = "${'$'}{json-unit.regex}^[0-9]{4}-[0-9]{2}-[0-9]{2}T.*Z$"
            val normalizedExpected =
                expectedJson
                    .trimIndent()
                    .replace("<timestamp>", timestampMatcher)

            println("STATUS_RESPONSE_BODY=" + actualBody)
            assertJsonEquals(normalizedExpected, actualBody)
        }
}
