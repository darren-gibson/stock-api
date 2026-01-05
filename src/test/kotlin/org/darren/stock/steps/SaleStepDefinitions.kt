package org.darren.stock.steps

import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import net.javacrumbs.jsonunit.JsonMatchers.jsonEquals
import org.darren.stock.steps.helpers.SaleRequestHelper
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SaleStepDefinitions : KoinComponent {
    private val saleRequestHelper by inject<SaleRequestHelper>()
    private lateinit var response: HttpResponse

    @When("there is a sale of {quantity} of {string} in the {string} store")
    fun thereIsASaleOfProductInStore(
        quantity: Double,
        productId: String,
        locationId: String,
    ) = runTest {
        response = saleRequestHelper.performSale(locationId, productId, quantity)
        assertTrue(response.status.isSuccess(), "Failed to create sale: ${response.status} ${response.bodyAsText()}")
    }

    @Then("the sale of {string} in {string} will result in a HTTP Status of {} and error {string}")
    fun theSaleOfProductInLocationWillResultIn(
        productId: String,
        locationId: String,
        status: Int,
        expectedError: String,
    ) = runTest {
        val expectedStatus = HttpStatusCode.fromValue(status)
        val response = saleRequestHelper.performSale(locationId, productId, 1.0)
        assertEquals(expectedStatus, response.status)

        if (!expectedStatus.isSuccess()) {
            val expectedBody = """{ "status": "$expectedError" }"""
            val actualBody = response.bodyAsText()

            assertThat(actualBody, jsonEquals(expectedBody))
        }
    }
}
