package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.LocationNotFoundException
import org.darren.stock.domain.stockSystem.GetValue.getValue
import org.darren.stock.domain.stockSystem.StockSystem
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime
import java.time.LocalDateTime.now

object GetStock {
    fun Routing.getStock() {
        val locations by inject<LocationApiClient>(LocationApiClient::class.java)

        get("/locations/{locationId}/products/{productId}") {
            val stockSystem = inject<StockSystem>(StockSystem::class.java).value
            val locationId = call.parameters["locationId"]!!
            val productId = call.parameters["productId"]!!

            try {
                locations.ensureValidLocations(locationId)

                val quantity = stockSystem.getValue(locationId, productId)
                call.respond(OK, GetStockResponseDTO.from(locationId, productId, quantity))

            } catch (e: LocationNotFoundException) {
                call.respond(NotFound, ErrorDTO("LocationNotFound"))
            }
        }
    }

    @Serializable
    private data class GetStockResponseDTO(
        val locationId: String,
        val productId: String,
        val quantity: Double,
        @Serializable(with = DateSerializer::class)
        val lastUpdated: LocalDateTime
    ) {
        companion object {
            fun from(locationId: String, productId: String, quantity: Double): GetStockResponseDTO {
                return GetStockResponseDTO(locationId, productId, quantity, now())
            }
        }
    }
}
