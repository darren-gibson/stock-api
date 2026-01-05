package org.darren.stock.ktor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.util.currentTraceId
import java.time.Instant

object Status {
    private val logger = KotlinLogging.logger {}

    fun Routing.statusEndpoint() {
        get("/_status") {
            logger.info { "Status check requested, traceId=${currentTraceId()}" }
            call.respond(
                HttpStatusCode.OK,
                StatusDTO(
                    status = StatusDTO.StatusCode.Healthy,
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
        }
    }
}
