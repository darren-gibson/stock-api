package com.darren.stock.steps

import com.darren.stock.domain.OperationNotSupportedException
import com.darren.stock.domain.stockSystem.GetValue.getValue
import com.darren.stock.domain.stockSystem.StockSystem
import com.darren.stock.domain.stockSystem.delivery
import com.darren.stock.domain.stockSystem.sale
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime.now
import java.util.*

class StockActorStepDefinitions : KoinComponent {
    private val stock by inject<StockSystem>()

    @Given("the stock level of {string} in {string} is {double}")
    @Given("a product {string} exists in {string} with a stock level of {double}")
    fun theStockLevelOfProductInStoreIs(productId: String, locationId: String, quantity: Double) = runBlocking {
        val url = "/locations/$locationId/products/$productId/counts"
        val requestId = UUID.randomUUID().toString()
        val payload = """{"requestId": "$requestId", "reason": "AdminOverride", "quantity": $quantity}"""

        ApiCallStepDefinitions().sendPostRequest(url, payload)
    }

    @When("^there is a delivery of (\\d+) (\\S+) to (\\S+) store$")
    fun thereIsADeliveryOfProductToStore(quantity: Double, productId: String, locationId: String) = runBlocking {
        stock.delivery(locationId, productId, quantity, now())
    }

    @When("there is a sale of {double} {string} in the {string} store")
    fun thereIsASaleOfProductInStore(quantity: Double, productId: String, locationId: String) = runBlocking {
        stock.sale(locationId, productId, quantity, now())
    }

    @Then("the current stock level of {string} in {string} will equal {double}")
    @Then("the stock level of product {string} in {string} should be updated to {double}")
    fun theCurrentStockLevelOfProductInStoreWillEqual(productId: String, locationId: String, expectedQuantity: Double) =
        runBlocking {
            val actualStock = getStockLevel(locationId, productId)
            assertEquals(expectedQuantity, actualStock)
        }

    private suspend fun getStockLevel(locationId: String, productId: String): Double {
        return stock.getValue(locationId, productId)
    }

    @Then("the sale of {string} in {string} will result in {string}")
    fun theSaleOfProductInLocationWillResultIn(productId: String, locationId: String, result: String) = runBlocking {
        if (result == "failure")
            assertThrows<OperationNotSupportedException> { stock.sale(locationId, productId, 1.0, now()) }
        else
            stock.sale(locationId, productId, 1.0, now())
    }
}