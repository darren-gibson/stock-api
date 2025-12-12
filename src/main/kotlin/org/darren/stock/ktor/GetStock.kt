package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockLevel
import org.darren.stock.domain.stockSystem.GetValue.getValue
import org.darren.stock.domain.stockSystem.StockSystem
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object GetStock {
    fun Routing.getStock() {
        get("/locations/{locationId}/products/{productId}") {
            val locations by inject<LocationApiClient>(LocationApiClient::class.java)
            val stockSystem by inject<StockSystem>(StockSystem::class.java)
            val locationId = call.parameters["locationId"]!!
            val productId = call.parameters["productId"]!!
            val includeChildren = call.parameters["includeChildren"]?.toBoolean() ?: true

            locations.ensureValidLocations(locationId)

            val stockLevel = stockSystem.getValue(locationId, productId, includeChildren)

            with(stockLevel) {
                if (includeChildren) {
                    call.respond(
                        OK,
                        GetStockResponseDTO(
                            locationId,
                            productId,
                            quantity,
                            pendingAdjustment,
                            lastUpdated,
                            totalQuantity,
                            childLocations.map(ChildLocationsDTO::from),
                        ),
                    )
                } else {
                    call.respond(
                        OK,
                        GetStockResponseDTO(locationId, productId, quantity, pendingAdjustment, lastUpdated),
                    )
                }
            }
        }
    }

    @Serializable
    private data class GetStockResponseDTO(
        val locationId: String,
        val productId: String,
        val quantity: Double? = null,
        val pendingAdjustment: Double = 0.0,
        @Serializable(with = DateSerializer::class)
        val lastUpdated: LocalDateTime,
        val totalQuantity: Double? = null,
        val childLocations: List<ChildLocationsDTO> = emptyList(),
    )

    @Serializable
    data class ChildLocationsDTO(
        val locationId: String,
        val quantity: Double? = null,
        val pendingAdjustment: Double = 0.0,
        val totalQuantity: Double?,
        val childLocations: List<ChildLocationsDTO> = emptyList(),
    ) {
        companion object {
            fun from(stockLevel: StockLevel): ChildLocationsDTO {
                with(stockLevel) {
                    return ChildLocationsDTO(
                        locationId,
                        quantity,
                        pendingAdjustment,
                        totalQuantity,
                        childLocations.map(ChildLocationsDTO::from),
                    )
                }
            }
        }
    }
}
