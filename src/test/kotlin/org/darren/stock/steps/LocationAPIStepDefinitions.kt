package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LocationAPIStepDefinitions : KoinComponent {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    init {
        val serviceHelper by inject<ServiceLifecycleSteps>()
        serviceHelper.getLocationByIdResponder = this@LocationAPIStepDefinitions::mockGetLocationByIdApi
        serviceHelper.getChildrenByIdResponder = this@LocationAPIStepDefinitions::mockGetChildrenByIdApi
    }

    private val locations = mutableMapOf<String, SimpleLocation>()

    @Given("the following locations exist:")
    @Given("the following locations are defined in the Location API:")
    fun theFollowingLocationsAreDefinedInTheLocationAPI(locationsToDefine: List<SimpleLocation>) = runBlocking {
        locations.putAll(locationsToDefine.map { it.id to it })
    }

    @Given("{string} is a store")
    @Given("{string} is a Distribution Centre")
    fun is_a_store(locationId: String): Unit = runBlocking {
        createLocationForTest(locationId, "Shop")
    }

    private fun createLocationForTest(locationId: String, role: String? = null) {
        locations[locationId] =
            if (role == null) SimpleLocation(locationId, "Store")
            else SimpleLocation(locationId, role)
    }

    @Given("{string} does not exist as a store")
    @Given("{string} does not exist as a Distribution Centre")
    @Given("an invalid location {string} is provided")
    @Given("{string} does not exist as a location")
    fun anInvalidLocationIsProvided(locationId: String) {
        locations.remove(locationId)
    }

    suspend fun mockGetLocationByIdApi(call: RoutingCall) = mockGetChildrenByIdApi(call)

    suspend fun mockGetChildrenByIdApi(call: RoutingCall) {
        logger.debug { "test called with route=${call.route} params=${call.pathParameters}" }
        val locationId = call.pathParameters["id"]
        val location = locations[locationId]
        if (location != null) {
            val bodyText = toLocation(location, !call.queryParameters.contains("depth"))
            logger.debug { "Returning $bodyText" }
            call.respondText(bodyText, ContentType.Application.Json)
        } else
            call.respond(HttpStatusCode.NotFound)
    }

    private fun toLocation(loc: SimpleLocation, includeChildren: Boolean = true) =
        """{
            "id": "${loc.id}",
            "name": "${loc.id}",
            "roles": ["${loc.role}"],
            "createdAt": "2024-12-15T12:34:56Z"
            ${if (includeChildren) ""","children": [${childLocations(loc)}]""" else ""}
        }"""

    private fun childLocations(loc: SimpleLocation): String {
        return locations.filter { it.value.parent == loc.id }
            .map { toLocation(it.value) }.joinToString(", ")
    }

    @DataTableType
    fun locationEntryTransformer(row: Map<String?, String>): SimpleLocation {
        return SimpleLocation(row["Location Id"]!!, row["Role"]!!, row["Parent Location Id"])
    }

    data class SimpleLocation(val id: String, val role: String = "Shop", val parent: String? = null)

    @Given("{string} is a {string} location")
    fun isALocation(locationId: String, role: String) {
        createLocationForTest(locationId, role)
    }

    @And("{string} is a receiving location in the network")
    @And("{string} is a receiving location")
    fun isAValidReceivingLocationInTheNetwork(locationId: String) {
        theFollowingLocationsAreDefinedInTheLocationAPI(listOf(SimpleLocation(locationId, "Zone")))
    }
}