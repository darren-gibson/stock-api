package org.darren.stock.ktor

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

object Status {
    fun Routing.statusEndpoint() {
        get("/_status") {
            call.respond(HttpStatusCode.OK, StatusDTO.healthy())
        }
    }

    @Serializable
    private data class StatusDTO(val status: StatusCode) {
        enum class StatusCode {
            Healthy
        }
        companion object {
            fun healthy() = StatusDTO(StatusCode.Healthy)
        }
    }
}