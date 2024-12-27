package org.darren.stock.steps

import io.cucumber.java.PendingException
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import net.javacrumbs.jsonunit.JsonMatchers.jsonEquals
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.koin.core.component.KoinComponent
import java.util.*

class StockActorStepDefinitions : KoinComponent {
    private lateinit var response: HttpResponse

    @Given("a Distribution Centre {string} with {double} units of {string}")
    @Given("a Store {string} with {double} units of {string}")
    fun aStoreWithUnitsOf(locationId: String, quantity: Double, productId: String) = runBlocking {
        theStockLevelOfProductInStoreIs(productId, locationId, quantity)
    }

    @Given("the stock level of {string} in {string} is {double}")
    @Given("a product {string} exists in {string} with a stock level of {double}")
    fun theStockLevelOfProductInStoreIs(productId: String, locationId: String, quantity: Double) = runBlocking {
        val url = "/locations/$locationId/products/$productId/counts"
        val requestId = UUID.randomUUID().toString()
        val payload = """{"requestId": "$requestId", "reason": "AdminOverride", "quantity": $quantity}"""

        ApiCallStepDefinitions().sendPostRequest(url, payload)
    }

    @When("^there is a delivery of (\\d+) (\\S+) to (\\S+) store$")
    fun thereIsADeliveryOfProductToStore(quantity: Double, productId: String, locationId: String) {
        throw PendingException("Not implemented")
//        stock.delivery(locationId, productId, quantity, now())
    }

    @When("there is a sale of {double} {string} in the {string} store")
    fun thereIsASaleOfProductInStore(quantity: Double, productId: String, locationId: String): HttpResponse =
        runBlocking {
            val url = "/stores/$locationId/products/$productId/sales"
            val requestId = UUID.randomUUID().toString()
            val payload = """{ "requestId": "$requestId", "quantity": $quantity }"""

            response = ApiCallStepDefinitions().sendPostRequest(url, payload)
            return@runBlocking response
        }

    @Then("the sale of {string} in {string} will result in a HTTP Status of {} and error {string}")
    fun theSaleOfProductInLocationWillResultIn(
        productId: String, locationId: String, status: Int, expectedError: String
    ) = runBlocking {
        val expectedStatus = HttpStatusCode.fromValue(status)
        val response = thereIsASaleOfProductInStore(1.0, productId, locationId)
        assertEquals(expectedStatus, response.status)

        if (!expectedStatus.isSuccess()) {
            val expectedBody = """{ "status": "$expectedError" }"""
            val actualBody = response.bodyAsText()

            assertThat(actualBody, jsonEquals(expectedBody))
        }
    }
}