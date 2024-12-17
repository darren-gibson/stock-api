package org.darren.stock.ktor

import org.darren.stock.domain.LocationNotFoundException
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.domain.stockSystem.delivery
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


fun Routing.delivery() {
    post("/locations/{locationId}/deliveries") {
        val stockSystem = inject<StockSystem>(StockSystem::class.java).value
        val sourceLocationId = call.parameters["locationId"]!!

        val request = call.receive<DeliveryRequestDTO>()

        try {
            with(request) {
                stockSystem.delivery(sourceLocationId, productId, quantity, now())
                call.respond(Created, DeliveryResponseDTO(requestId, sourceLocationId, destinationLocationId,
                    productId, quantity, now()))
            }
        } catch (e: LocationNotFoundException) {
            call.respond(NotFound, ErrorDTO("LocationNotFound"))
        }
    }
}

@Serializable
data class DeliveryRequestDTO(
    val requestId: String,
    val productId: String,
    val quantity: Double,
    val destinationLocationId: String
)

@Serializable
data class DeliveryResponseDTO(
    val requestId: String,
    val sourceLocationId: String,
    val destinationLocationId: String,
    val productId: String,
    val quantityDelivered: Double,
    @Serializable(with = DateSerializer::class)
    val deliveryTimestamp: LocalDateTime
)