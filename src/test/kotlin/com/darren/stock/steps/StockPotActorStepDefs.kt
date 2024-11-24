package com.darren.stock.steps

import com.darren.stock.domain.StockMessages
import com.darren.stock.domain.StockMessages.*
import com.darren.stock.domain.actors.stockPotActor
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import java.time.LocalDateTime
import kotlin.test.assertEquals

class StockPotActorStepDefs {
    private val stockPot = mutableMapOf<String, SendChannel<StockMessages>>()

    @Given("^the stock level of (\\S+) in (\\S+) store is (\\d+)$")
    fun theStockLevelOfProductInStoreIs(productId: String, locationId: String, quantity: Double) {
        stockPot[locationId] = GlobalScope.stockPotActor()
    }

    @When("^there is a delivery of (\\d+) (\\S+) to (\\S+) store$")
    fun thereIsADeliveryOfProductToStore(quantity: Double, productId: String, locationId: String) = runBlocking {
        val stockPot = stockPot[locationId]!!
        stockPot.send(DeliveryEvent(LocalDateTime.now(), locationId, productId, quantity))
    }

    @Then("^the current stock level of (\\S+) in (\\S+) store will equal (\\d+)$")
    fun theCurrentStockLevelOfProductInStoreWillEqual(productId: String, locationId: String, expectedQuantity: Double) =
        runBlocking {
            val stockPot = stockPot[locationId]!!
            val actualStock = getStockLevel(stockPot, locationId, productId)
            assertEquals(expectedQuantity, actualStock)
        }

    private suspend fun getStockLevel(
        stockPot: SendChannel<StockMessages>,
        locationId: String,
        productId: String
    ): Double {
        val deferred = CompletableDeferred<Double>()
        stockPot.send(GetValue(locationId, productId, deferred))
        return deferred.await()
    }
}