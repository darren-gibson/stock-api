package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.LocationNotFoundException
import org.darren.stock.domain.ProductQuantity
import org.darren.stock.domain.stockSystem.Delivery.delivery
import org.darren.stock.domain.stockSystem.StockSystem
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

object Delivery {
    @OptIn(ExperimentalSerializationApi::class)
    fun Routing.delivery() {
        val locations by inject<LocationApiClient>(LocationApiClient::class.java)

        post("/locations/{locationId}/deliveries") {
            val stockSystem = inject<StockSystem>(StockSystem::class.java).value
            val locationId = call.parameters["locationId"]!!


            try {
                val request = call.receive<DeliveryRequestDTO>()
                locations.ensureValidLocations(locationId)

                with(request) {
                    stockSystem.delivery(locationId, supplierId, supplierRef, deliveryDate, products.productQuantity())
                    call.respond(Created)
                }
            } catch (e: LocationNotFoundException) {
                call.respond(NotFound, ErrorDTO("LocationNotFound"))
            } catch (e: BadRequestException) {
                if (e.cause?.cause is MissingFieldException) {
                    val missing: MissingFieldException = e.cause?.cause as MissingFieldException
                    call.respond(BadRequest, MissingFieldsDTO(missing.missingFields))
                } else {
                    throw e
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
        val deliveryDate: LocalDateTime,
        val products: List<ProductDTO>,
    )

    @Serializable
    data class ProductDTO(val productId: String, val quantity: Double)

    private fun List<ProductDTO>.productQuantity(): List<ProductQuantity> {
        return this.map { ProductQuantity(it.productId, it.quantity) }
    }
}