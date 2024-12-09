package com.darren.stock.ktor

import com.darren.stock.domain.LocationNotFoundException
import com.darren.stock.domain.stockSystem.StockSystem
import com.darren.stock.domain.stockSystem.sale
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


fun Routing.sale() {
    post("/stores/{locationId}/products/{productId}/sales") {
        val stockSystem = inject<StockSystem>(StockSystem::class.java)
        val locationId = call.parameters["locationId"]!!
        val productId = call.parameters["productId"]!!

        val request = call.receive<SaleRequestDTO>()

        try {
            with(request) {
                stockSystem.value.sale(locationId, productId, quantity, now())
                call.respond(Created, SaleResponseDTO(requestId, locationId, productId, quantity, now()))
            }
        } catch (e: LocationNotFoundException) {
            call.respond(NotFound, ErrorDTO("LocationNotFound"))
        }
    }
}

@Serializable
data class SaleRequestDTO(val requestId: String, val quantity: Double)

@Serializable
data class SaleResponseDTO(
    val requestId: String,
    val location: String,
    val productId: String,
    val quantitySold: Double,
    @Serializable(with = DateSerializer::class)
    val saleTimestamp: LocalDateTime
)
