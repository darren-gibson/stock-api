package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationNotTrackedException
import org.darren.stock.domain.MovementReason
import org.darren.stock.domain.stockSystem.Move.recordMovement
import org.darren.stock.domain.stockSystem.MoveCommand
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.auth.Permission
import org.darren.stock.ktor.auth.requiresAuth
import org.darren.stock.util.DateSerializer
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object Move {
    fun Routing.moveEndpoint() {
        route("/locations/{sourceLocationId}/{productId}/movements") {
            requiresAuth(Permission("stock", "movement", "write"), "sourceLocationId")

            post {
                val sourceId = call.parameters["sourceLocationId"]!!
                val productId = call.parameters["productId"]!!
                val stockSystem by inject<StockSystem>(StockSystem::class.java)

                val request = call.receive<MoveRequestDTO>()

                try {
                    with(request) {
                        val command =
                            MoveCommand(
                                fromLocationId = sourceId,
                                toLocationId = destinationLocationId,
                                productId = productId,
                                quantity = quantity,
                                reason = reason,
                                movedAt = movedAt,
                                requestId = requestId,
                            )
                        stockSystem.recordMovement(command)
                        call.respond(
                            Created,
                            MoveResponseDTO(
                                requestId,
                                sourceId,
                                destinationLocationId,
                                productId,
                                quantity,
                                reason,
                                movedAt,
                            ),
                        )
                    }
                } catch (e: LocationNotTrackedException) {
                    if (e.locationId == request.destinationLocationId) {
                        call.respond(BadRequest, ErrorDTO("LocationNotTracked"))
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    @Serializable
    private data class MoveRequestDTO(
        val requestId: String,
        val destinationLocationId: String,
        val quantity: Double,
        val reason: MovementReason,
        @Serializable(with = DateSerializer::class)
        val movedAt: LocalDateTime,
    )

    @Serializable
    private data class MoveResponseDTO(
        val requestId: String,
        val sourceLocationId: String,
        val destinationLocationId: String,
        val productId: String,
        val quantity: Double,
        val reason: MovementReason,
        @Serializable(with = DateSerializer::class)
        val movedAt: LocalDateTime,
    )
}
