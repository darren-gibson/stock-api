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
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.LocationNotFoundException
import org.darren.stock.domain.OperationNotSupportedException
import org.darren.stock.domain.stockSystem.Sale.sale
import org.darren.stock.domain.stockSystem.StockSystem
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

object Sale {
    @OptIn(ExperimentalSerializationApi::class)
    fun Routing.sale() {
        val locations by inject<LocationApiClient>(LocationApiClient::class.java)

        post("/stores/{locationId}/products/{productId}/sales") {
            val stockSystem = inject<StockSystem>(StockSystem::class.java).value
            val locationId = call.parameters["locationId"]!!
            val productId = call.parameters["productId"]!!

            try {
                val request = call.receive<SaleRequestDTO>()
                locations.ensureValidLocation(locationId)

                with(request) {
                    stockSystem.sale(locationId, productId, quantity, soldAt)
                    call.respond(Created, SaleResponseDTO(requestId, locationId, productId, quantity, soldAt))
                }
            } catch (e: LocationNotFoundException) {
                call.respond(NotFound, ErrorDTO("LocationNotFound"))
            } catch (e: OperationNotSupportedException) {
                call.respond(Conflict, ErrorDTO("LocationNotSupported"))
            } catch (e: BadRequestException) {
                if (e.cause?.cause is MissingFieldException) {
                    val missing: MissingFieldException = e.cause?.cause as MissingFieldException
                    call.respond(BadRequest, MissingFieldsDTO(missing.missingFields))
                } else if(e.cause?.cause is DateTimeParseException) {
                    call.respond(BadRequest, InvalidValuesDTO(listOf("soldAt")))
                } else {
                    throw e
                }
            }
        }
    }

    @Serializable
    private data class SaleRequestDTO(
        val requestId: String, val quantity: Double,
        @Serializable(with = DateSerializer::class) val soldAt: LocalDateTime
    )

    @Serializable
    private data class SaleResponseDTO(
        val requestId: String,
        val location: String,
        val productId: String,
        val quantitySold: Double,
        @Serializable(with = DateSerializer::class)
        val soldAt: LocalDateTime
    )
}
