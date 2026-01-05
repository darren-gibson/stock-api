package org.darren.stock.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.steps.helpers.TestStockEventRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class IdempotencyStepDefinitions : KoinComponent {
    private val repository: StockEventRepository by inject()
    private val client: HttpClient by inject()
    private var previousResponse: String? = null

    @Given("the stock system will fail with a {int} error on the first request")
    @Suppress("UNUSED_PARAMETER")
    fun theStockSystemWillFailOnFirstRequest(statusCode: Int) {
        // Configure the test repository to fail on the next insert
        (repository as? TestStockEventRepository)?.failNextInserts(1)
    }

    @When("the stock system is working normally")
    fun theStockSystemIsWorkingNormally() {
        // Reset the test repository to normal operation
        (repository as? TestStockEventRepository)?.resetFailureSimulation()
    }

    @Then("the response should be identical to the previous response")
    fun theResponseShouldBeIdenticalToPrevious() {
        val currentResponse = TestContext.lastResponseBody
        assertEquals(previousResponse, currentResponse, "Duplicate request should return identical response")
    }

    @When("an out-of-order stock adjustment event occurs for {string} in {string} with quantity {double} at {string}")
    fun anOutOfOrderStockAdjustmentEventOccurs(
        product: String,
        location: String,
        quantity: Double,
        countedAt: String,
    ) = runTest {
        // Make an explicit POST request to the stock count endpoint with an out-of-order timestamp
        val payload =
            """
            {
                "requestId": "out-of-order-adjustment-001",
                "reason": "AdminOverride",
                "quantity": $quantity,
                "countedAt": "$countedAt"
            }
            """.trimIndent()

        val response =
            client.post("/locations/$location/products/$product/counts") {
                contentType(ContentType.Application.Json)
                setBody(payload)
                TestContext.getAuthorizationToken()?.let { token ->
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
            }

        // The response should be successful
        assertEquals(201, response.status.value)
    }

    @When("an out-of-order stock sale event occurs for {string} in {string} with quantity {double} at {string}")
    fun anOutOfOrderStockSaleEventOccurs(
        product: String,
        location: String,
        quantity: Double,
        soldAt: String,
    ) = runTest {
        // Make an explicit POST request to the sales endpoint with an out-of-order timestamp
        val payload =
            """
            {
                "requestId": "out-of-order-sale-001",
                "quantity": $quantity,
                "soldAt": "$soldAt"
            }
            """.trimIndent()

        val response =
            client.post("/locations/$location/products/$product/sales") {
                contentType(ContentType.Application.Json)
                setBody(payload)
                TestContext.getAuthorizationToken()?.let { token ->
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
            }

        // The response should be successful
        assertEquals(201, response.status.value)
    }

    @Then("store the response for comparison")
    fun storeResponseForComparison() {
        previousResponse = TestContext.lastResponseBody
    }
}
