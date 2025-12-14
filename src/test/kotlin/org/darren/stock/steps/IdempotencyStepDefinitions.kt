package org.darren.stock.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.ktor.idempotency.IdempotencyStore
import org.darren.stock.steps.helpers.TestStockEventRepository
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class IdempotencyStepDefinitions : KoinComponent {
    private val idempotencyStore: IdempotencyStore by inject()
    private val repository: StockEventRepository by inject()

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

    @Then("the response should be cached in the idempotency store")
    fun theResponseShouldBeCached() {
        val lastRequestBody = TestContext.lastRequestBody
        val requestId = extractRequestIdFromJson(lastRequestBody)

        assertNotNull(requestId, "RequestId should be present in request")
        val cachedResponse = idempotencyStore.get(requestId!!)
        assertNotNull(cachedResponse, "Response should be cached for requestId: $requestId")
    }

    @Then("the response should not be cached in the idempotency store")
    fun theResponseShouldNotBeCached() {
        val lastRequestBody = TestContext.lastRequestBody
        val requestId = extractRequestIdFromJson(lastRequestBody)

        assertNotNull(requestId, "RequestId should be present in request")
        val cachedResponse = idempotencyStore.get(requestId!!)
        assertNull(cachedResponse, "Response should NOT be cached for requestId: $requestId")
    }

    private fun extractRequestIdFromJson(json: String): String? {
        val regex = """"requestId"\s*:\s*"([^"]+)"""".toRegex()
        val match = regex.find(json)
        return match?.groupValues?.get(1)
    }
}
