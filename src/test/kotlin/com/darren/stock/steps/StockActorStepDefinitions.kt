package com.darren.stock.steps

import com.darren.stock.domain.OperationNotSupportedException
import com.darren.stock.domain.StockSystem
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime.now

class StockActorStepDefinitions : KoinComponent {
    private val testApp: TestApplication by inject()
    private val client = testApp.client
    private val stock by inject<StockSystem>()

    @Given("^the stock level of (\\S+) in ([\\S ]+) (?:store )?is (\\d+)")
    fun theStockLevelOfProductInStoreIs(productId: String, locationId: String, quantity: Double) = runBlocking {
        stock.setInitialStockLevel(locationId, productId, quantity)
    }

    @When("^there is a delivery of (\\d+) (\\S+) to (\\S+) store$")
    fun thereIsADeliveryOfProductToStore(quantity: Double, productId: String, locationId: String) = runBlocking {
        stock.delivery(locationId, productId, quantity, now())
    }

    @When("^there is a sale of (\\d+) (\\S+) in the (\\S+) store$")
    fun thereIsASaleOfProductInStore(quantity: Double, productId: String, locationId: String) = runBlocking {
        stock.sale(locationId, productId, quantity, now())
    }
    @Then("the current stock level of {string} in {string} will equal {double}")
    fun theCurrentStockLevelOfProductInStoreWillEqual(productId: String, locationId: String, expectedQuantity: Double) =
        runBlocking {
            val actualStock = getStockLevel(locationId, productId)
            assertEquals(expectedQuantity, actualStock)
        }

    private suspend fun getStockLevel(locationId: String, productId: String): Double {
        return stock.getValue(locationId, productId)
    }

    @Then("the sale of {} in {} will result in {}")
    fun theSaleOfProductInLocationWillResultIn(productId: String, locationId: String, result: String) = runBlocking {
        if (result == "failure")
            assertThrows<OperationNotSupportedException> { stock.sale(locationId, productId, 1.0, now()) }
        else
            stock.sale(locationId, productId, 1.0, now())
    }
}