package org.darren.stock.domain

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import org.darren.stock.domain.resilience.ApiResilienceConfig
import org.darren.stock.domain.resilience.ApiResilienceManager
import org.darren.stock.util.wrapHttpCallWithLogging

class LocationApiClient(
    private val baseUrl: String,
    engine: HttpClientEngine,
    resilienceManager: ApiResilienceManager,
) : ResilientApiClient(engine),
    HealthProbe {
    private var currentResilienceConfig: ApiResilienceConfig = resilienceManager.currentConfig()

    init {
        logger = KotlinLogging.logger {}
        client = buildClient(currentResilienceConfig)
        resilienceManager.onConfigChanged {
            currentResilienceConfig = it
            updateClient(it)
        }
    }

    suspend fun ensureValidLocation(locationId: String) = getLocation(locationId)

    suspend fun getLocationsHierarchy(
        locationId: String,
        depth: Int? = null,
    ): LocationDTO =
        executeRequest(
            getHierarchyUri(depth, locationId),
            locationId,
        )

    private fun getHierarchyUri(
        depth: Int?,
        locationId: String,
    ) = "$baseUrl/locations/$locationId/children${if (depth != null) "?depth=$depth" else ""}"

    private suspend fun getLocation(locationId: String): LocationDTO = executeRequest("$baseUrl/locations/$locationId", locationId)

    suspend fun getPath(locationId: String): Set<LocationDTO> = executeRequest("$baseUrl/locations/$locationId/path", locationId)

    suspend fun ensureLocationsAreTracked(vararg locations: String) =
        locations.forEach {
            val location = getLocation(it)
            if (!location.isTracked) throw LocationNotTrackedException(it)
        }

    override suspend fun isHealthy(): Boolean =
        runCatching {
            val response: HttpResponse =
                wrapHttpCallWithLogging(logger) {
                    client.get("$baseUrl/health")
                }
            response.status.isSuccess()
        }.onFailure { logger.warn(it) { "Location API health check failed: ${it.message}" } }
            .getOrDefault(false)

    override fun getResilienceConfig(): ApiResilienceConfig = currentResilienceConfig

    override fun handleResponseException(
        status: HttpStatusCode,
        context: String,
    ): Nothing {
        if (status == HttpStatusCode.NotFound) {
            throw LocationNotFoundException(context)
        }
        throw LocationApiUnavailableException(status.toString())
    }

    @Serializable
    data class LocationDTO(
        val id: String = "",
        val roles: Set<String> = emptySet(),
        val children: List<LocationDTO> = emptyList(),
    ) {
        val isTracked
            get() = roles.contains(LocationRoles.TrackedInventoryLocation.name)
    }
}
