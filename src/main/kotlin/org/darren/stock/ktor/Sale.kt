package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.stockSystem.Sale.recordSale
import org.darren.stock.domain.stockSystem.StockSystem
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object Sale {
    fun Routing.saleEndpoint() {
        post("/locations/{locationId}/products/{productId}/sales") {
            val stockSystem by inject<StockSystem>(StockSystem::class.java)
            val locationId = call.parameters["locationId"]!!
            val productId = call.parameters["productId"]!!
            val request = call.receive<SaleRequestDTO>()

            with(request) {
                stockSystem.recordSale(locationId, productId, quantity, soldAt)
                call.respond(Created, SaleResponseDTO(requestId, locationId, productId, quantity, soldAt))
            }
        }
    }

    @Serializable
    private data class SaleRequestDTO(
        val requestId: String,
        val quantity: Double,
        @Serializable(with = DateSerializer::class) val soldAt: LocalDateTime,
    )

    @Serializable
    private data class SaleResponseDTO(
        val requestId: String,
        val locationId: String,
        val productId: String,
        val quantitySold: Double,
        @Serializable(with = DateSerializer::class)
        val soldAt: LocalDateTime,
    )
}
