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

            // Define checks declaratively and evaluate in a DRY, uniform way
            val checks =
                runHealthChecks(
                    listOf(
                        "locationApi" to
                            suspend {
                                val locations by inject<LocationApiClient>(LocationApiClient::class.java)
                                locations.isHealthy()
                            },
                        "eventRepository" to
                            suspend {
                                val eventRepo by inject<StockEventRepository>(StockEventRepository::class.java)
                                eventRepo.isHealthy()
                            },
                        "snapshotRepository" to
                            suspend {
                                val snapshotRepo by inject<SnapshotRepository>(SnapshotRepository::class.java)
                                snapshotRepo.isHealthy()
                            },
                    ),
                )

            val allHealthy = checks.all { it.status == ProbeStatus.UP }
            val probeStatus = if (allHealthy) ProbeStatus.UP else ProbeStatus.DOWN
            val httpStatus = if (allHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable

            call.respond(
                httpStatus,
                HealthProbeResponse(
                    status = probeStatus,
                    checks = checks,
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

    // Helper to run health checks uniformly with logging and error safety
    private suspend fun runHealthChecks(
        checks: List<Pair<String, suspend () -> Boolean>>,
    ): List<HealthCheck> =
        checks.map { (name, check) ->
            val ok =
                runCatching { check() }
                    .onFailure { logger.warn(it) { "Health check '$name' failed with exception" } }
                    .getOrDefault(false)

            if (!ok) {
                logger.warn { "Health check '$name' reported DOWN" }
            }

            HealthCheck(name = name, status = if (ok) ProbeStatus.UP else ProbeStatus.DOWN)
        }
}
