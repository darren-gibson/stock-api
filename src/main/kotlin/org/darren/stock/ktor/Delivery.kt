package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darren.stock.domain.ProductQuantity
import org.darren.stock.domain.stockSystem.Delivery.recordDelivery
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.auth.Permission
import org.darren.stock.ktor.auth.requiresAuth
import org.darren.stock.ktor.idempotency.idempotent
import org.darren.stock.ktor.idempotency.receiveAndCheckDuplicate
import org.darren.stock.ktor.idempotency.respondIdempotent
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object Delivery {
    fun Routing.deliveryEndpoint() {
        route("/locations/{locationId}/deliveries") {
            requiresAuth(Permission("stock", "movement", "write"), "locationId")
            idempotent()

            post {
                val locationId = call.parameters["locationId"]!!
                val stockSystem by inject<StockSystem>(StockSystem::class.java)

                // Receive body and check for duplicates
                val request = call.receiveAndCheckDuplicate<DeliveryRequestDTO> { it.requestId } ?: return@post

                with(request) {
                    stockSystem.recordDelivery(locationId, supplierId, supplierRef, deliveredAt, products.productQuantity())
                    call.respondIdempotent(Created)
                }
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
