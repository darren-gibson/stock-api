package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.PendingException
import io.cucumber.java.en.Given
import kotlinx.coroutines.runBlocking
import org.darren.stock.domain.LocationType
import org.koin.core.component.KoinComponent

class LocationManagementSteps : KoinComponent {
    @Given("^(\\S+) is a shelf in (\\S+) store")
    fun location_is_a_shelf_in_store(locationId: String, parentLocationId: String): Unit = runBlocking {
        throw PendingException()
//        locations.send(DefineLocationEvent(locationId, LocationType.Untracked, now(), parentLocationId))
    }

    @Given("the following locations exit:")
    fun theFollowingLocationsExit(locationTable: List<Location>): Unit = runBlocking {
        throw PendingException()
        locationTable.forEach {
//            locations.send(DefineLocationEvent(it.location, it.locationType, now(), it.parentLocationId))
        }
    }

    data class Location(val location: String, val parentLocationId: String?, val locationType: LocationType)

    @DataTableType
    fun locationEntryTransformer(row: Map<String?, String>): Location {
        throw PendingException()
        return Location(row["location"]!!, row["parentLocation"], LocationType.valueOf(row["type"]!!))
    }

//    @Given("{} is a {} location")
//    @Given("{string} is an {} store")
//    fun isALocation(locationId: String, type: LocationType): Unit = runBlocking {
//        throw PendingException()
////        locations.send(DefineLocationEvent(locationId, type, now(), null))
//    }
}