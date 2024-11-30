package com.darren.stock.steps

import com.darren.stock.domain.LocationMessages
import com.darren.stock.domain.LocationType
import com.darren.stock.domain.actors.LocationActor.Companion.locationActor
import com.darren.stock.domain.StockSystem
import io.cucumber.java.DataTableType
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.*
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFails

@OptIn(DelicateCoroutinesApi::class)
class StockActorStepDefinitions {
    private val locations = GlobalScope.locationActor()
    private val stock = StockSystem(locations)

    @Given("^the stock level of (\\S+) in ([\\S ]+) (?:store )?is (\\d+)")
    fun theStockLevelOfProductInStoreIs(productId: String, locationId: String, quantity: Double) = runBlocking {
        stock.setInitialStockLevel(locationId, productId, quantity)
    }

    @When("^there is a delivery of (\\d+) (\\S+) to (\\S+) store$")
    fun thereIsADeliveryOfProductToStore(quantity: Double, productId: String, locationId: String) = runBlocking {
        stock.delivery(locationId, productId, quantity, LocalDateTime.now())
    }

    @When("^there is a sale of (\\d+) (\\S+) in the (\\S+) store$")
    fun thereIsASaleOfProductInStore(quantity: Double, productId: String, locationId: String) = runBlocking {
        stock.sale(locationId, productId, quantity, LocalDateTime.now())
    }

    @Then("^the current stock level of ([\\S ]+) in ([\\S ]+) will equal (\\d+)")
    fun theCurrentStockLevelOfProductInStoreWillEqual(productId: String, locationId: String, expectedQuantity: Double) =
        runBlocking {
            val actualStock = getStockLevel(locationId, productId)
            assertEquals(expectedQuantity, actualStock)
        }

    @Given("^(\\S+) is a store")
    fun is_a_store(locationId: String) = runBlocking {
        locations.send(LocationMessages.DefineLocationEvent(locationId, LocationType.Tracked, LocalDateTime.now(), null))
    }

    @Given("^(\\S+) is a shelf in (\\S+) store")
    fun location_is_a_shelf_in_store(locationId: String, parentLocationId: String) = runBlocking {
        locations.send(LocationMessages.DefineLocationEvent(locationId, LocationType.Untracked, LocalDateTime.now(), parentLocationId))
    }

    private suspend fun getStockLevel(locationId: String, productId: String): Double {
        return stock.getValue(locationId, productId)
    }

    @Given("the following locations exit:")
    fun theFollowingLocationsExit(locationTable: List<Location>) = runBlocking {
        locationTable.forEach {
            locations.send(LocationMessages.DefineLocationEvent(
                it.location,
                it.locationType,
                LocalDateTime.now(),
                it.parentLocationId
            ))
        }
    }

    data class Location(val location: String, val parentLocationId: String?, val locationType: LocationType)

    @DataTableType
    fun locationEntryTransformer(row: Map<String?, String>): Location {
        return Location(row["location"]!!, row["parentLocation"], LocationType.valueOf(row["type"]!!))
    }

    @Then("the sale of {} in {} will result in {}")
    fun theSaleOfProductInLocationWillResultIn(productId: String, locationId: String, result: String) = runBlocking {
        if (result == "failure")
            assertFails { stock.sale(locationId, productId, 1.0, LocalDateTime.now()) }
        else
            stock.sale(locationId, productId, 1.0, LocalDateTime.now())
    }

    @Given("{} is a {} location")
    fun isALocation(locationId: String, type: LocationType) = runBlocking {
        locations.send(LocationMessages.DefineLocationEvent(locationId, type, LocalDateTime.now(), null))
    }
}