package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.stockSystem.Sale.sale
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.ExceptionWrapper.runWithExceptionHandling
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object Sale {
    fun Routing.sale() {
        val locations by inject<LocationApiClient>(LocationApiClient::class.java)

        post("/locations/{locationId}/products/{productId}/sales") {
            val stockSystem = inject<StockSystem>(StockSystem::class.java).value
            val locationId = call.parameters["locationId"]!!
            val productId = call.parameters["productId"]!!
            runWithExceptionHandling(call, "soldAt") {
                val request = call.receive<SaleRequestDTO>()
                locations.ensureValidLocation(locationId)

                with(request) {
                    stockSystem.sale(locationId, productId, quantity, soldAt)
                    call.respond(Created, SaleResponseDTO(requestId, locationId, productId, quantity, soldAt))
                }
            }
        }
    }

    @Serializable
    private data class SaleRequestDTO(
        val requestId: String, val quantity: Double,
        @Serializable(with = DateSerializer::class) val soldAt: LocalDateTime
    )

    @Serializable
    private data class SaleResponseDTO(
        val requestId: String,
        val location: String,
        val productId: String,
        val quantitySold: Double,
        @Serializable(with = DateSerializer::class)
        val soldAt: LocalDateTime
    )
}
