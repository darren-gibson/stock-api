package com.darren.stock.ktor

import com.darren.stock.domain.StockCountReason
import com.darren.stock.domain.StockSystem
import io.ktor.http.*
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
        val locationId = call.parameters["locationId"]
        val productId = call.parameters["productId"]

//        assert(stockSystem.isInitialized())

        // Validate path parameters
//            if (locationId.isNullOrBlank() || productCode.isNullOrBlank()) {
//                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Invalid locationId or productCode"))
//                return@post
//            }

        val request = call.receive<StockCountRequestDTO>()

        with(request) {
            stockSystem.value.count(locationId!!, productId!!, quantity, reason, now())
        }
        // Parse the request payload
//            val request = try {
//                call.receive<Map<String, Any>>()
//            } catch (e: Exception) {
//                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Invalid JSON payload"))
//                return@post
//            }
//
//            val requestId = request["requestId"] as? String
//            val reason = request["reason"] as? String
//            val quantity = request["quantity"] as? Int

//            // Validate request payload
//            if (requestId.isNullOrBlank() || reason.isNullOrBlank() || quantity == null) {
//                call.respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to "Missing or invalid fields"))
//                return@post
//            }
//
//            // Handle idempotency
//            storeMutex.withLock {
//                if (requestIdStore.containsKey(requestId)) {
//                    val existingRecord = requestIdStore[requestId]!!
//                    call.respond(HttpStatusCode.OK, mapOf(
//                        "status" to "duplicate_request",
//                        "message" to "The request has already been processed.",
//                        "processedAt" to existingRecord.createdAt
//                    ))
//                    return@post
//                }
//
//                // Process the request
//                val stockCount = StockCount(
//                    requestId = requestId,
//                    locationId = locationId,
//                    productCode = productCode,
//                    reason = reason,
//                    quantity = quantity,
//                    createdAt = System.currentTimeMillis().toString() // Use proper date formatting
//                )
//
//                requestIdStore[requestId] = stockCount
//                call.respond(HttpStatusCode.Created, mapOf(
//                    "status" to "success",
//                    "message" to "Stock count created successfully.",
//                    "data" to stockCount
//                ))
//            }
//        }
//    }
        with(request) {
            call.respond(
                HttpStatusCode.Created,
                StockCountResponseDTO(requestId, locationId!!, productId!!, quantity, reason, now())
            )
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