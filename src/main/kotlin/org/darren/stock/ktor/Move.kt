package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import org.darren.stock.domain.*
import org.darren.stock.domain.stockSystem.Move.move
import org.darren.stock.domain.stockSystem.StockSystem
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

object Move {
    @OptIn(ExperimentalSerializationApi::class)
    fun Routing.move() {
        val locations by inject<LocationApiClient>(LocationApiClient::class.java)

        post("/locations/{sourceLocationId}/{productId}/movements") {
            val stockSystem = inject<StockSystem>(StockSystem::class.java).value
            val sourceId = call.parameters["sourceLocationId"]!!
            val productId = call.parameters["productId"]!!

            try {
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

            } catch (e: LocationNotFoundException) {
                call.respond(NotFound, ErrorDTO("LocationNotFound"))
            } catch (e: OperationNotSupportedException) {
                call.respond(Conflict, ErrorDTO("LocationNotSupported"))
            } catch (e: BadRequestException) {
                if (e.cause?.cause is MissingFieldException) {
                    val missing: MissingFieldException = e.cause?.cause as MissingFieldException
                    call.respond(BadRequest, MissingFieldsDTO(missing.missingFields))
                } else if (e.cause?.cause is DateTimeParseException) {
                    call.respond(BadRequest, InvalidValuesDTO(listOf("movedAt")))
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
