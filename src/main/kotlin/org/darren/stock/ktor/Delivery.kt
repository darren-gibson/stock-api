package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.ProductQuantity
import org.darren.stock.domain.stockSystem.Delivery.delivery
import org.darren.stock.domain.stockSystem.StockSystem
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object Delivery {
    fun Routing.delivery() {
        post("/locations/{locationId}/deliveries") {
            val stockSystem = inject<StockSystem>(StockSystem::class.java).value
            val locationId = call.parameters["locationId"]!!
            val request = call.receive<DeliveryRequestDTO>()

            with(request) {
                stockSystem.delivery(locationId, supplierId, supplierRef, deliveredAt, products.productQuantity())
                call.respond(Created)
            }
        }
    }

    @Serializable
    private data class DeliveryRequestDTO(
        val requestId: String,
        val supplierId: String,
        val supplierRef: String,
        @Serializable(with = DateSerializer::class)
        val deliveredAt: LocalDateTime,
        val products: List<ProductDTO>,
    )

    @Serializable
    data class ProductDTO(
        val productId: String,
        val quantity: Double,
    )

    private fun List<ProductDTO>.productQuantity(): List<ProductQuantity> = this.map { ProductQuantity(it.productId, it.quantity) }
}
