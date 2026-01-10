package org.darren.stock.ktor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.util.currentTraceId
import org.koin.java.KoinJavaComponent.inject
import java.time.Instant

object Status {
    private val logger = KotlinLogging.logger {}

    fun Routing.statusEndpoint() {
        get("/_status") {
            val locations by inject<LocationApiClient>(LocationApiClient::class.java)
            logger.info { "Status check requested, traceId=${currentTraceId()}" }
            val downstreamHealthy = locations.isHealthy()
            if (!downstreamHealthy) {
                logger.warn { "Downstream Location API health check failed" }
            }
            val httpStatus = if (downstreamHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            call.respond(
                httpStatus,
                StatusDTO(
                    status =
                        if (downstreamHealthy) {
                            StatusDTO.StatusCode.Healthy
                        } else {
                            StatusDTO.StatusCode.Unhealthy
                        },
                    version = System.getProperty("app.version") ?: "unknown",
                    buildTime = System.getProperty("app.buildTime") ?: Instant.now().toString(),
                ),
            )
        }
    }

    @Serializable
    private data class StatusDTO(
        val status: StatusCode,
        val version: String,
        val buildTime: String,
    ) {
        enum class StatusCode {
            Healthy,
            Unhealthy,
        }
    }
}
