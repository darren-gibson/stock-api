package org.darren.stock.domain

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LocationApiClient(private val baseUrl: String) : KoinComponent {
    private val engine by inject<HttpClientEngine>()
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                explicitNulls = false
            })
        }
    }

    suspend fun ensureValidLocation(locationId: String) = getLocation(locationId)

    suspend fun ensureValidLocations(vararg locations: String) = locations.forEach { ensureValidLocation(it) }

    suspend fun getLocationsHierarchy(locationId: String, depth: Int? = null): LocationDTO {
        val response = client.get(getHierarchyUri(depth, locationId))
        if(response.status.isSuccess())
            return response.body<LocationDTO>()
        throw LocationNotFoundException(locationId)
    }

    private fun getHierarchyUri(depth: Int?, locationId: String) =
        if (depth != null) {
            "${baseUrl}/locations/$locationId/children?depth=$depth"
        } else {
            "${baseUrl}/locations/$locationId/children"
        }

    suspend fun isShop(locationId: String): Boolean {
        return getLocation(locationId).isShop
    }

    private suspend fun getLocation(locationId: String): LocationDTO {
        val response = client.get("${baseUrl}/locations/$locationId")
        if (response.status.isSuccess())
            return response.body<LocationDTO>()
        throw LocationNotFoundException(locationId)
    }

    @Serializable
    data class LocationDTO(val id: String, val roles: Set<String>, val children: List<LocationDTO> = emptyList()) {
        val isShop
            get() = roles.contains(LocationRoles.Shop.name)
    }
}