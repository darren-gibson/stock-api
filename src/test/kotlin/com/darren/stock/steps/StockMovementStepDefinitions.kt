package com.darren.stock.steps

import com.darren.stock.domain.*
import com.darren.stock.domain.LocationMessages.*
import com.darren.stock.domain.actors.LocationActor.Companion.locationActor
import io.cucumber.java.DataTableType
import io.cucumber.java.en.Given
import io.cucumber.java.en.And
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertInstanceOf
import java.time.LocalDateTime
import kotlin.test.assertEquals

class StockMovementStepDefinitions {
    private var lastException: Exception? = null

    @OptIn(DelicateCoroutinesApi::class)
    private val locations = GlobalScope.locationActor()
    private val stock = StockSystem(locations)

    @Given("a Distribution Centre {string} with {double} units of {string}")
    fun aDistributionCentreWithUnitsOf(locationId: String, quantity: Double, productId: String) = runBlocking {
        createTrackedLocation(locationId)
        setStockLevel(locationId, productId, quantity)
    }

    private suspend fun setStockLevel(locationId: String, productId: String, quantity: Double) {
        stock.setInitialStockLevel(locationId, productId, quantity)
    }

    @And("a Store {string} with {double} units of {string}")
    fun aStoreWithUnitsOf(locationId: String, quantity: Double, productId: String) = runBlocking {
        createTrackedLocation(locationId)
        setStockLevel(locationId, productId, quantity)
    }

    private suspend fun createTrackedLocation(locationId: String) {
        locations.send(DefineLocationEvent(locationId, LocationType.Tracked, LocalDateTime.now(), null))
    }

    @When("I initiate a stock movement transaction with the following details:")
    fun iInitiateAStockMovementTransactionWithTheFollowingDetails(stockMovements: List<StockMovement>) = runBlocking {
        stockMovements.forEach { movement ->
            try {
                stock.move(movement)
            } catch (e: Exception) {
                lastException = e
            }
        }
    }

    @Then("the move request should fail with an InsufficientStock exception")
    fun theMoveRequestShouldFailWithAnInsufficientStockException() {
        assertInstanceOf<InsufficientStockException>(lastException)
    }

    @Then("the stock levels should be updated as follows:")
    @And("the stock levels should remain unchanged:")
    fun theStockLevelsShouldBeUpdatedAsFollows(expectedStockLevels: List<ExpectedStock>) = runBlocking {
        expectedStockLevels.forEach { expected ->
            val actual = getStockLevel(expected.location, expected.product)
            assertEquals(expected.quantity, actual)
        }
    }

    private suspend fun getStockLevel(locationId: String, productId: String): Double {
        return stock.getValue(locationId, productId)
    }

    @DataTableType
    fun locationEntryTransformer(row: Map<String?, String>) =
        StockMovement(
            row["source"]!!, row["destination"]!!, row["product"]!!,
            row["quantity"]!!.toDouble(), StockMovementReason.valueOf(row["reason"]!!)
        )

    data class ExpectedStock(val location: String, val product: String, val quantity: Double)

    @DataTableType
    fun expectedStockTransformer(row: Map<String?, String>) =
        ExpectedStock(row["location"]!!, row["product"]!!, row["quantity"]!!.toDouble())

}