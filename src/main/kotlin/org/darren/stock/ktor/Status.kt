package org.darren.stock.ktor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.snapshot.SnapshotRepository
import org.darren.stock.util.currentTraceId
import org.koin.java.KoinJavaComponent.inject

object Status {
    private val logger = KotlinLogging.logger {}

    fun Routing.statusEndpoint() {
        liveProbe()
        readyProbe()
        startupProbe()
    }

    private fun Routing.liveProbe() {
        get("/health/live") {
            logger.info { "Liveness probe check requested, traceId=${currentTraceId()}" }
            call.respond(
                HttpStatusCode.OK,
                HealthProbeResponse(
                    status = ProbeStatus.UP,
                    checks = emptyList(),
                ),
            )
        }
    }

    private fun Routing.readyProbe() {
        get("/health/ready") {
            logger.info { "Readiness probe check requested, traceId=${currentTraceId()}" }

            val locationApiHealthy =
                runCatching {
                    val locations by inject<LocationApiClient>(LocationApiClient::class.java)
                    locations.isHealthy()
                }.onFailure { logger.warn(it) { "Failed to check Location API health" } }.getOrDefault(false)

            val eventRepoHealthy =
                runCatching {
                    val eventRepo by inject<StockEventRepository>(StockEventRepository::class.java)
                    eventRepo.isHealthy()
                }.onFailure { logger.warn(it) { "Failed to check event repository health" } }.getOrDefault(false)

            val snapshotRepoHealthy =
                runCatching {
                    val snapshotRepo by inject<SnapshotRepository>(SnapshotRepository::class.java)
                    snapshotRepo.isHealthy()
                }.onFailure { logger.warn(it) { "Failed to check snapshot repository health" } }.getOrDefault(false)

            if (!locationApiHealthy) {
                logger.warn { "Downstream Location API health check failed" }
            }
            if (!eventRepoHealthy) {
                logger.warn { "Event repository health check failed" }
            }
            if (!snapshotRepoHealthy) {
                logger.warn { "Snapshot repository health check failed" }
            }

            val allHealthy = locationApiHealthy && eventRepoHealthy && snapshotRepoHealthy
            val probeStatus = if (allHealthy) ProbeStatus.UP else ProbeStatus.DOWN
            val httpStatus = if (allHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable

            call.respond(
                httpStatus,
                HealthProbeResponse(
                    status = probeStatus,
                    checks =
                        listOf(
                            HealthCheck(name = "locationApi", status = if (locationApiHealthy) ProbeStatus.UP else ProbeStatus.DOWN),
                            HealthCheck(name = "eventRepository", status = if (eventRepoHealthy) ProbeStatus.UP else ProbeStatus.DOWN),
                            HealthCheck(
                                name = "snapshotRepository",
                                status = if (snapshotRepoHealthy) ProbeStatus.UP else ProbeStatus.DOWN,
                            ),
                        ),
                ),
            )
        }
    }

    private fun Routing.startupProbe() {
        get("/health/started") {
            logger.info { "Startup probe check requested, traceId=${currentTraceId()}" }
            call.respond(
                HttpStatusCode.OK,
                HealthProbeResponse(
                    status = ProbeStatus.UP,
                    checks = emptyList(),
                ),
            )
        }
    }

    @Serializable
    enum class ProbeStatus {
        @SerialName("UP")
        UP,

        @SerialName("DOWN")
        DOWN,
    }

    @Serializable
    private data class HealthProbeResponse(
        val status: ProbeStatus,
        val checks: List<HealthCheck>,
    )

    @Serializable
    private data class HealthCheck(
        val name: String,
        val status: ProbeStatus,
    )
}
