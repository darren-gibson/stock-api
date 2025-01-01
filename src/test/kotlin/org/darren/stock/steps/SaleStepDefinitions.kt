package org.darren.stock.steps

import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import net.javacrumbs.jsonunit.JsonMatchers.jsonEquals
import org.darren.stock.steps.helpers.TestDateTimeProvider
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class SaleStepDefinitions : KoinComponent {
    private lateinit var response: HttpResponse
    private val apiCallStepDefinitions by inject<ApiCallStepDefinitions>()
    private val dateTimeProvider: TestDateTimeProvider by inject()

    @When("there is a sale of {quantity} of {string} in the {string} store")
    fun thereIsASaleOfProductInStore(quantity: Double, productId: String, locationId: String) = runBlocking {
        response = performSale(locationId, productId, quantity)
        assertTrue(response.status.isSuccess(), "Failed to create sale: ${response.status} ${response.bodyAsText()}")
    }

    private fun performSale(
        locationId: String,
        productId: String,
        quantity: Double
    ) = runBlocking {
        val url = "/stores/$locationId/products/$productId/sales"
        val requestId = UUID.randomUUID().toString()
        val soldAt = dateTimeProvider.nowAsString()
        val payload = """{ "requestId": "$requestId", "soldAt": "$soldAt", "quantity": $quantity }"""

        return@runBlocking apiCallStepDefinitions.sendPostRequest(url, payload)
    }

    @Then("the sale of {string} in {string} will result in a HTTP Status of {} and error {string}")
    fun theSaleOfProductInLocationWillResultIn(
        productId: String, locationId: String, status: Int, expectedError: String
    ) = runBlocking {
        val expectedStatus = HttpStatusCode.fromValue(status)
        val response = performSale(locationId, productId, 1.0)
        assertEquals(expectedStatus, response.status)

        if (!expectedStatus.isSuccess()) {
            val expectedBody = """{ "status": "$expectedError" }"""
            val actualBody = response.bodyAsText()

            assertThat(actualBody, jsonEquals(expectedBody))
        }
    }
}