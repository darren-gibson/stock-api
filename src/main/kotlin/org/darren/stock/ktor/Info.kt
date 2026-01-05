package org.darren.stock.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.time.Instant

object Info {
    @Serializable
    private data class InfoResponse(
        val service: String,
        val version: String,
        val buildTime: String,
        val status: String,
    )

    fun Routing.infoEndpoint() {
        get("/info") {
            val version = System.getProperty("app.version") ?: "unknown"
            val buildTime = System.getProperty("app.buildTime") ?: Instant.now().toString()

            call.respond(
                HttpStatusCode.OK,
                InfoResponse(
                    service = "Stock API",
                    version = version,
                    buildTime = buildTime,
                    status = "running",
                ),
            )
        }
    }
}
