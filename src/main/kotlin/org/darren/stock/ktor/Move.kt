package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationNotTrackedException
import org.darren.stock.domain.MovementReason
import org.darren.stock.domain.stockSystem.Move.move
import org.darren.stock.domain.stockSystem.StockSystem
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object Move {
    fun Routing.move() {
        post("/locations/{sourceLocationId}/{productId}/movements") {
            val stockSystem by inject<StockSystem>(StockSystem::class.java)
            val sourceId = call.parameters["sourceLocationId"]!!
            val productId = call.parameters["productId"]!!

            val request = call.receive<MoveRequestDTO>()

            try {
                with(request) {
                    stockSystem.move(sourceId, destinationLocationId, productId, quantity, reason, movedAt)
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
