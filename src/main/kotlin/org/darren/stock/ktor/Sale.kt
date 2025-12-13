package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.stockSystem.Sale.recordSale
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.auth.JwtConfig
import org.darren.stock.ktor.auth.Permission
import org.darren.stock.ktor.auth.authenticate
import org.darren.stock.ktor.auth.authorize
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object Sale {
    fun Routing.saleEndpoint() {
        post("/locations/{locationId}/products/{productId}/sales") {
            val jwtConfig by inject<JwtConfig>(JwtConfig::class.java)
            if (call.authenticate(jwtConfig) == null) return@post

            val locationId = call.parameters["locationId"]!!
            val productId = call.parameters["productId"]!!

            if (!call.authorize(Permission("stock", "movement", "write"), locationId)) return@post

            val stockSystem by inject<StockSystem>(StockSystem::class.java)
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
