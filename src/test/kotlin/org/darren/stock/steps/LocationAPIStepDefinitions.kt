package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.http.CacheControl.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocationAPIStepDefinitions : KoinComponent {
    private val trackedInventoryRoleName = "TrackedInventoryLocation"
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    init {
        val serviceHelper by inject<ServiceLifecycleSteps>()
        serviceHelper.getLocationByIdResponder = this@LocationAPIStepDefinitions::mockGetLocationByIdApi
        serviceHelper.getChildrenByIdResponder = this@LocationAPIStepDefinitions::mockGetChildrenByIdApi
    }

    private val locations = mutableMapOf<String, SimpleLocation>()
    private val countOfCallsByLocation = mutableMapOf<String, Int>()
    private val cacheControlByLocation = mutableMapOf<String, String>()

    @Given("the following tracked locations exist:")
    @Given("the following locations exist:")
    @Given("the following tracked locations are defined in the Location API:")
    fun theFollowingLocationsAreDefinedInTheLocationAPI(locationsToDefine: List<SimpleLocation>) = runBlocking {
        locations.putAll(locationsToDefine.map { it.id to it })
    }

    @Given("{string} is a store")
    @Given("{string} is a Distribution Centre")
    fun is_a_store(locationId: String): Unit = runBlocking {
        createLocationForTest(locationId, "Shop")
    }

    private fun createLocationForTest(locationId: String, role: String) {
        locations[locationId] = SimpleLocation(locationId, listOf(role, trackedInventoryRoleName))
    }

    @Given("{string} does not exist as a store")
    @Given("{string} does not exist as a Distribution Centre")
    @Given("an invalid location {string} is provided")
    @Given("{string} does not exist as a location")
    fun anInvalidLocationIsProvided(locationId: String) {
        locations.remove(locationId)
    }

    suspend fun mockGetLocationByIdApi(call: RoutingCall) {
        val locationId = call.pathParameters["id"]!!
        countOfCallsByLocation[locationId] = countOfCallsByLocation.getOrDefault(locationId, 0) + 1
        respondToGetChildrenByIdCall(call)
    }

    suspend fun mockGetChildrenByIdApi(call: RoutingCall) {
        respondToGetChildrenByIdCall(call)
    }

    private suspend fun respondToGetChildrenByIdCall(call: RoutingCall) {
        val locationId = call.pathParameters["id"]!!
        logger.debug { "test called with route=${call.route} params=${call.pathParameters}, count=${countOfCallsByLocation[locationId]}" }
        val location = locations[locationId]
        if (location != null) {
            call.response.cacheControl(getCacheControlForLocation(locationId))
            val bodyText = toLocation(location, !call.queryParameters.contains("depth"))
            logger.debug { "Returning $bodyText, with cache=${getCacheControlForLocation(locationId)}" }
            call.respondText(bodyText, ContentType.Application.Json)
        } else
            call.respond(HttpStatusCode.NotFound)
    }

    private fun getCacheControlForLocation(locationId: String): CacheControl {
        val value = cacheControlByLocation.getOrDefault(locationId, "no-cache")
        value.split("=").let { parts ->
            when (parts[0]) {
                "no-cache" -> return NoCache(null)
                "max-age" -> return MaxAge(parts[1].toInt())
                else -> return NoCache(null)
            }
        }
    }

    private fun toLocation(loc: SimpleLocation, includeChildren: Boolean = true) =
        """{
            "id": "${loc.id}",
            "name": "${loc.id}",
            "roles": [${loc.roles.joinToString(separator = ",") { "\"${it}\"" }}],
            "createdAt": "2024-12-15T12:34:56Z"
            ${if (includeChildren) ""","children": [${childLocations(loc)}]""" else ""}
        }"""

    private fun childLocations(loc: SimpleLocation): String {
        return locations.filter { it.value.parent == loc.id }
            .map { toLocation(it.value) }.joinToString(", ")
    }

    @DataTableType
    fun locationEntryTransformer(row: Map<String?, String>): SimpleLocation {
        val roles = if(row.containsKey("Roles"))
            row["Roles"]?.split(",")?.map { it.trim() } ?: emptyList()
        else listOf(trackedInventoryRoleName)

        return SimpleLocation(row["Location Id"]!!, roles, row["Parent Location Id"])
    }

    data class SimpleLocation(val id: String, val roles: List<String> = emptyList(), val parent: String? = null)

    @Given("{string} is a {string} location")
    fun isALocation(locationId: String, role: String) {
        createLocationForTest(locationId, role)
    }

    @And("{string} is a receiving location in the network")
    @And("{string} is a receiving location")
    fun isAValidReceivingLocationInTheNetwork(locationId: String) {
        theFollowingLocationsAreDefinedInTheLocationAPI(listOf(SimpleLocation(locationId, listOf("Zone", trackedInventoryRoleName))))
    }

    @And("{string} is moved to {string}")
    fun isMovedTo(locationId: String, newParentLocationId: String) {
        locations[locationId] = locations[locationId]!!.copy(parent = newParentLocationId)
    }

    @Given("the Location API responds to get location requests with the following cache-control header:")
    fun theLocationAPIRespondsToGetLocationRequestsWithTheFollowingCacheControlHeader(setting: List<CacheControlSetting>) {
        cacheControlByLocation.putAll(setting.map { it.id to it.header })
        setting.forEach { createLocationForTest(it.id, trackedInventoryRoleName) }
    }

    @DataTableType
    fun cacheControlSettingTransformer(row: Map<String?, String>): CacheControlSetting {
        return CacheControlSetting(row["Location Id"]!!, row["Cache-Control"]!!)
    }

    data class CacheControlSetting(val id: String, val header: String)

    @Then("the Location API should have been called no more than once for {string}")
    fun theLocationAPIShouldHaveBeenCalledNoMoreThanOnceFor(locationId: String) {
        assertEquals(1, countOfCallsByLocation.getOrDefault(locationId, 0))
    }

    @Then("the Location API should have been called more than once for {string}")
    fun theLocationAPIShouldHaveBeenCalledMoreThanOnceFor(locationId: String) {
        assertTrue(countOfCallsByLocation.getOrDefault(locationId, 0) > 1)
    }
}