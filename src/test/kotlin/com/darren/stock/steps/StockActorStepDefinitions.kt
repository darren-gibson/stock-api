package com.darren.stock.steps

import com.darren.stock.domain.StockMessages.*
import com.darren.stock.domain.actors.StockActor.Companion.stockActor
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.*
import java.time.LocalDateTime
import kotlin.test.assertEquals

@OptIn(DelicateCoroutinesApi::class)
class StockActorStepDefinitions {
    private val stock = GlobalScope.stockActor()

    @Given("^the stock level of (\\S+) in (\\S+) store is (\\d+)$")
    fun theStockLevelOfProductInStoreIs(productId: String, locationId: String, quantity: Double) = runBlocking {
        stock.send(SetStockLevelEvent(locationId, productId, LocalDateTime.now(), quantity))
//        stockPot[locationId to productId] = GlobalScope.stockPotActor(locationId, productId, quantity)
    }

    @When("^there is a delivery of (\\d+) (\\S+) to (\\S+) store$")
    fun thereIsADeliveryOfProductToStore(quantity: Double, productId: String, locationId: String) = runBlocking {
        stock.send(DeliveryEvent(locationId, productId, LocalDateTime.now(), quantity))
    }

    @When("^there is a sale of (\\d+) (\\S+) in the (\\S+) store$")
    fun thereIsASaleOfProductInStore(quantity: Double, productId: String, locationId: String) = runBlocking {
        stock.send(SaleEvent(locationId, productId, LocalDateTime.now(), quantity))
    }

    @Then("^the current stock level of (\\S+) in (\\S+) store will equal (\\d+)$")
    fun theCurrentStockLevelOfProductInStoreWillEqual(productId: String, locationId: String, expectedQuantity: Double) =
        runBlocking {
            val actualStock = getStockLevel(locationId, productId)
            assertEquals(expectedQuantity, actualStock)
        }

    private suspend fun getStockLevel(locationId: String, productId: String): Double {
        val deferred = CompletableDeferred<Double>()
        stock.send(GetValue(locationId, productId, deferred))
        return deferred.await()
    }
}