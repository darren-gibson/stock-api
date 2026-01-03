package org.darren.stock.ktor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.util.currentTraceId

object Status {
    private val logger = KotlinLogging.logger {}

    fun Routing.statusEndpoint() {
        get("/_status") {
            logger.info { "Status check requested, traceId=${currentTraceId()}" }
            call.respond(HttpStatusCode.OK, StatusDTO.healthy())
        }
    }

    @Serializable
    private data class StatusDTO(
        val status: StatusCode,
    ) {
        enum class StatusCode {
            Healthy,
        }

        companion object {
            fun healthy() = StatusDTO(StatusCode.Healthy)
        }
    }
}
