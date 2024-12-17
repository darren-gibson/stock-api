package org.darren.stock.ktor

import org.darren.stock.domain.LocationNotFoundException
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.domain.stockSystem.count
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime
import java.time.LocalDateTime.now

fun Routing.stockCount() {
    post("/locations/{locationId}/products/{productId}/counts") {
        val stockSystem = inject<StockSystem>(StockSystem::class.java)
        val locationId = call.parameters["locationId"]!!
        val productId = call.parameters["productId"]!!

        val request = call.receive<StockCountRequestDTO>()

        try {
            with(request) {
                stockSystem.value.count(locationId, productId, quantity, reason, now())
                call.respond(Created, StockCountResponseDTO(requestId, locationId, productId, quantity, reason, now()))
            }
        } catch (e: LocationNotFoundException) {
            call.respond(NotFound, ErrorDTO("LocationNotFound"))
        }
    }
}

@Serializable
data class StockCountRequestDTO(val requestId: String, val reason: StockCountReason, val quantity: Double)

@Serializable
data class StockCountResponseDTO(
    val requestId: String,
    val location: String,
    val productId: String,
    val quantity: Double,
    val reason: StockCountReason,
    @Serializable(with = DateSerializer::class)
    val createdAt: LocalDateTime
)