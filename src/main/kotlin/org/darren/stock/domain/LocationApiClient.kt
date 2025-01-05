package org.darren.stock.domain

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.darren.stock.util.LoggingHelper.wrapHttpCallWithLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LocationApiClient(private val baseUrl: String) : KoinComponent {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val engine by inject<HttpClientEngine>()
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // only depends on the fields we need, enables forward compatibility
                coerceInputValues = true // enable case insensitive deserialization
                explicitNulls = false
            })
        }
        install(HttpCache)
    }

    suspend fun ensureValidLocation(locationId: String) = getLocation(locationId)

    suspend fun ensureValidLocations(vararg locations: String) = locations.forEach { ensureValidLocation(it) }

    suspend fun getLocationsHierarchy(locationId: String, depth: Int? = null): LocationDTO {
        val response = wrapHttpCallWithLogging(logger) { client.get(getHierarchyUri(depth, locationId)) }
        if (response.status.isSuccess())
            return response.body<LocationDTO>()
        throw LocationNotFoundException(locationId)
    }

    private fun getHierarchyUri(depth: Int?, locationId: String) =
        "$baseUrl/locations/$locationId/children${if (depth != null) "?depth=$depth" else "" }"

    suspend fun isShop(locationId: String): Boolean {
        return getLocation(locationId).isShop
    }

    private suspend fun getLocation(locationId: String): LocationDTO {
        val response = wrapHttpCallWithLogging(logger) { client.get("${baseUrl}/locations/$locationId") }
        if (response.status.isSuccess())
            return response.body<LocationDTO>()
        throw LocationNotFoundException(locationId)
    }

    suspend fun getPath(locationId: String): Set<LocationDTO> {
        val response = wrapHttpCallWithLogging(logger) { client.get("${baseUrl}/locations/$locationId/path") }
        if (response.status.isSuccess())
            return response.body<Set<LocationDTO>>()
        throw LocationNotFoundException(locationId)
    }

    suspend fun ensureLocationsAreTracked(vararg locations: String) = locations.forEach {
        val location =        getLocation(it)
        if (!location.isTracked) throw LocationNotTrackedException(it)
    }

    @Serializable
    data class LocationDTO(val id: String, val roles: Set<String>, val children: List<LocationDTO> = emptyList()) {
        val isShop
            get() = roles.contains(LocationRoles.Shop.name)
        val isTracked
            get() = roles.contains(LocationRoles.TrackedInventoryLocation.name)
    }
}