package org.darren.stock.ktor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationApiClient
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
            val locations by inject<LocationApiClient>(LocationApiClient::class.java)
            logger.info { "Liveness probe check requested, traceId=${currentTraceId()}" }
            call.respond(
                HttpStatusCode.OK,
                HealthProbeResponse(
                    status = "UP",
                    checks = emptyList(),
                ),
            )
        }
    }

    private fun Routing.readyProbe() {
        get("/health/ready") {
            val locations by inject<LocationApiClient>(LocationApiClient::class.java)
            logger.info { "Readiness probe check requested, traceId=${currentTraceId()}" }
            val downstreamHealthy = locations.isHealthy()
            if (!downstreamHealthy) {
                logger.warn { "Downstream Location API health check failed" }
            }
            val httpStatus = if (downstreamHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            call.respond(
                httpStatus,
                HealthProbeResponse(
                    status = if (downstreamHealthy) "UP" else "DOWN",
                    checks =
                        listOf(
                            HealthCheck(
                                name = "locationApi",
                                status = if (downstreamHealthy) "UP" else "DOWN",
                            ),
                        ),
                ),
            )
        }
    }

    private fun Routing.startupProbe() {
        get("/health/started") {
            val locations by inject<LocationApiClient>(LocationApiClient::class.java)
            logger.info { "Startup probe check requested, traceId=${currentTraceId()}" }
            call.respond(
                HttpStatusCode.OK,
                HealthProbeResponse(
                    status = "UP",
                    checks = emptyList(),
                ),
            )
        }
    }

    @Serializable
    private data class HealthProbeResponse(
        val status: String,
        val checks: List<HealthCheck>,
    )

    @Serializable
    private data class HealthCheck(
        val name: String,
        val status: String,
    )
}
