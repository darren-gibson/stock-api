package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.stockSystem.CountRequest
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.domain.stockSystem.count
import org.darren.stock.ktor.auth.Permission
import org.darren.stock.ktor.auth.requiresAuth
import org.darren.stock.util.DateSerializer
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object StockCount {
    fun Routing.stockCountEndpoint() {
        route("/locations/{locationId}/products/{productId}/counts") {
            requiresAuth(Permission("stock", "count", "write"), "locationId")

            post {
                val locationId = call.parameters["locationId"]!!
                val productId = call.parameters["productId"]!!
                val locations by inject<LocationApiClient>(LocationApiClient::class.java)
                val stockSystem by inject<StockSystem>(StockSystem::class.java)

                val request = call.receive<StockCountRequestDTO>()

                locations.ensureValidLocation(locationId)
                with(request) {
                    stockSystem.count(CountRequest(locationId, productId, quantity, reason, countedAt, requestId))
                    call.respond(
                        Created,
                        StockCountResponseDTO(requestId, locationId, productId, quantity, reason, countedAt),
                    )
                }
            }
        }
    }

    @Serializable
    private data class StockCountRequestDTO(
        val requestId: String,
        val reason: StockCountReason,
        val quantity: Double,
        @Serializable(with = org.darren.stock.util.DateSerializer::class) val countedAt: LocalDateTime,
    )

    @Serializable
    private data class StockCountResponseDTO(
        val requestId: String,
        val location: String,
        val productId: String,
        val quantity: Double,
        val reason: StockCountReason,
        @Serializable(with = DateSerializer::class)
        val countedAt: LocalDateTime,
    )
}
