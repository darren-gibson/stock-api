package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.MovementReason
import org.darren.stock.domain.stockSystem.Move.move
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.ExceptionWrapper.runWithExceptionHandling
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object Move {
    fun Routing.move() {
        val locations by inject<LocationApiClient>(LocationApiClient::class.java)

        post("/locations/{sourceLocationId}/{productId}/movements") {
            val stockSystem = inject<StockSystem>(StockSystem::class.java).value
            val sourceId = call.parameters["sourceLocationId"]!!
            val productId = call.parameters["productId"]!!

            runWithExceptionHandling(call, "movedAt") {
                val request = call.receive<MoveRequestDTO>()
                locations.ensureValidLocations(sourceId, request.destinationLocationId)

                with(request) {
                    stockSystem.move(sourceId, destinationLocationId, productId, quantity, reason, movedAt)
                    call.respond(
                        Created,
                        MoveResponseDTO(
                            requestId, sourceId, destinationLocationId, productId, quantity, reason, movedAt
                        )
                    )
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
        val movedAt: LocalDateTime
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
        val movedAt: LocalDateTime
    )
}
