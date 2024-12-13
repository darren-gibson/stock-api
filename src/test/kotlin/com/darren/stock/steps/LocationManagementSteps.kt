package com.darren.stock.steps

import com.darren.stock.domain.actors.LocationMessages
import com.darren.stock.domain.actors.LocationMessages.DefineLocationEvent
import com.darren.stock.domain.LocationType
import io.cucumber.java.DataTableType
import io.cucumber.java.en.Given
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime.now

class LocationManagementSteps : KoinComponent {
    private val locations by inject<SendChannel<LocationMessages>>()

    @Given("{string} is a store")
    @Given("{string} is a Distribution Centre")
    fun is_a_store(locationId: String) = runBlocking {
        locations.send(DefineLocationEvent(locationId, LocationType.Tracked, now(), null))
    }

    @Given("^(\\S+) is a shelf in (\\S+) store")
    fun location_is_a_shelf_in_store(locationId: String, parentLocationId: String) = runBlocking {
        locations.send(DefineLocationEvent(locationId, LocationType.Untracked, now(), parentLocationId))
    }

    @Given("the following locations exit:")
    fun theFollowingLocationsExit(locationTable: List<Location>) = runBlocking {
        locationTable.forEach {
            locations.send(DefineLocationEvent(it.location, it.locationType, now(), it.parentLocationId))
        }
    }

    data class Location(val location: String, val parentLocationId: String?, val locationType: LocationType)

    @DataTableType
    fun locationEntryTransformer(row: Map<String?, String>): Location {
        return Location(row["location"]!!, row["parentLocation"], LocationType.valueOf(row["type"]!!))
    }

    @Given("{} is a {} location")
    @Given("{string} is an {} store")
    fun isALocation(locationId: String, type: LocationType) = runBlocking {
        locations.send(DefineLocationEvent(locationId, type, now(), null))
    }

    @Given("{string} does not exist as a store")
    @Given("an invalid location {string} is provided")
    fun anInvalidLocationIsProvided(locationId: String) {
    }
}