package org.darren.stock.steps

import io.cucumber.java.DataTableType
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

    private val serviceHelper by inject<ServiceLifecycleSteps>()
    private val locations = mutableMapOf<String, SimpleLocation>()

    @Given("the following locations are defined in the Location API:")
    fun theFollowingLocationsAreDefinedInTheLocationAPI(locationsToDefine: List<SimpleLocation>) = runBlocking {
        locations.putAll(locationsToDefine.map { it.id to it })
        serviceHelper.locationResponder = this@LocationAPIStepDefinitions::mockLocationApi
    }

    @Given("{string} is a store")
    @Given("{string} is a Distribution Centre")
    fun is_a_store(locationId: String): Unit = runBlocking {
        createLocationForTest(locationId)
    }

    private fun createLocationForTest(locationId: String, role: String? = null) {
        locations[locationId] =
            if (role == null) SimpleLocation(locationId, "Store")
            else SimpleLocation(locationId, "Store", role)
        serviceHelper.locationResponder = this@LocationAPIStepDefinitions::mockLocationApi
    }

    @Given("{string} does not exist as a store")
    @Given("{string} does not exist as a Distribution Centre")
    @Given("an invalid location {string} is provided")
    fun anInvalidLocationIsProvided(locationId: String) {
        locations.remove(locationId)
    }

    suspend fun mockLocationApi(call: RoutingCall) {
        logger.debug { "test called with call=${call.pathParameters}" }
        val locationId = call.pathParameters["id"]
        val location = locations[locationId]
        if (location != null)
            call.respondText(toLocation(location), ContentType.Application.Json)
        else call.respond(HttpStatusCode.NotFound)
    }

    private fun toLocation(loc: SimpleLocation) =
        """{
            "id": "${loc.id}",
            "type": "${loc.type}",
            "name": "${loc.id}",
            "roles": ["${loc.role}"],
            "createdAt": "2024-12-15T12:34:56Z"
        }"""


    @DataTableType
    fun locationEntryTransformer(row: Map<String?, String>): SimpleLocation {
        return SimpleLocation(row["id"]!!, row["type"]!!)
    }

    data class SimpleLocation(val id: String, val type: String, val role: String = "Shop")

    @Given("{string} is a {string} location")
    fun isALocation(locationId: String, role: String) {
        createLocationForTest(locationId, role)
    }
}