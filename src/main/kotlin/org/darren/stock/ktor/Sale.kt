package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.stockSystem.Sale.recordSale
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.auth.Permission
import org.darren.stock.ktor.auth.requiresAuth
import org.darren.stock.ktor.idempotency.idempotent
import org.darren.stock.ktor.idempotency.receiveAndCheckDuplicate
import org.darren.stock.ktor.idempotency.respondIdempotent
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object Sale {
    fun Routing.saleEndpoint() {
        route("/locations/{locationId}/products/{productId}/sales") {
            requiresAuth(Permission("stock", "movement", "write"), "locationId")
            idempotent()

            post {
                val locationId = call.parameters["locationId"]!!
                val productId = call.parameters["productId"]!!
                val stockSystem by inject<StockSystem>(StockSystem::class.java)

                // Receive body and check for duplicates (only if requestId is present)
                val request = call.receiveAndCheckDuplicate<SaleRequestDTO> { it.requestId } ?: return@post

                with(request) {
                    stockSystem.recordSale(locationId, productId, quantity, soldAt)
                    call.respondIdempotent(Created, SaleResponseDTO(requestId, locationId, productId, quantity, soldAt))
                }
            }
        }
    }

    @Serializable
    private data class SaleRequestDTO(
        val requestId: String,
        val quantity: Double,
        @Serializable(with = org.darren.stock.util.DateSerializer::class) val soldAt: LocalDateTime,
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
