package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.LocationNotFoundException
import org.darren.stock.domain.StockLevel
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
            val includeChildren = call.parameters["includeChildren"]?.toBoolean() ?: true

            try {
                locations.ensureValidLocations(locationId)

                val stockLevel = stockSystem.getValue(locationId, productId, includeChildren)

                with(stockLevel) {
                    if (includeChildren) {
                        call.respond(
                            OK, GetStockResponseDTO(
                                locationId, productId, quantity, now(), totalQuantity,
                                childLocations.map(ChildLocationsDTO::from)
                            )
                        )
                    } else {
                        call.respond(OK, GetStockResponseDTO(locationId, productId, quantity, now()))
                    }
                }


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
        val lastUpdated: LocalDateTime,
        val totalQuantity: Double? = null,
        val childLocations: List<ChildLocationsDTO> = emptyList()
    )

    @Serializable
    data class ChildLocationsDTO(
        val locationId: String, val quantity: Double, val totalQuantity: Double?,
        val childLocations: List<ChildLocationsDTO> = emptyList()
    ) {
        companion object {
            fun from(stockLevel: StockLevel): ChildLocationsDTO {
                with(stockLevel) {
                    return ChildLocationsDTO(
                        locationId, quantity, totalQuantity,
                        childLocations.map(ChildLocationsDTO::from)
                    )
                }
            }
        }
    }
}

