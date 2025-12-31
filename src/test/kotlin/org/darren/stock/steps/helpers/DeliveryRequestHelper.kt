package org.darren.stock.steps.helpers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.darren.stock.steps.TestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class DeliveryRequestHelper : KoinComponent {
    private val client: HttpClient by inject()
    private val dateTimeProvider: TestDateTimeProvider by inject()
    private val logger = KotlinLogging.logger {}

    suspend fun runDeliveryForProducts(
        locationId: String,
        vararg products: Pair<String, Double>,
        requestId: String = System.currentTimeMillis().toString(),
        deliveredAt: String = dateTimeProvider.nowAsString(),
    ): HttpResponse {
        val resp = callTheDeliveryEndpoint(products, deliveredAt, locationId, requestId)
        assertTrue(resp.status.isSuccess())
        return resp
    }

    private suspend fun callTheDeliveryEndpoint(
        products: Array<out Pair<String, Double>>,
        deliveredAt: String,
        locationId: String,
        requestId: String,
    ): HttpResponse {
        val payload = buildBody(products.toList(), deliveredAt, requestId)

        val resp =
            client.post("/locations/$locationId/deliveries") {
                setBody(payload)
                contentType(ContentType.Application.Json)
                TestContext.getAuthorizationToken()?.let { token ->
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
            }
        return resp
    }

    internal suspend fun runDeliveryAttemptForProducts(
        locationId: String,
        vararg products: Pair<String, Double>,
        deliveredAt: String = dateTimeProvider.nowAsString(),
        requestId: String = System.currentTimeMillis().toString(),
    ): Any {
        try {
            return callTheDeliveryEndpoint(products, deliveredAt, locationId, requestId)
        } catch (e: Exception) {
            logger.debug(e) { "Delivery request failed as expected" }
            return "failed"
        }
    }

    private fun buildBody(
        productQuantities: List<Pair<String, Double>>,
        deliveredAt: String,
        requestId: String,
    ): String {
        val products =
            productQuantities.joinToString(",") {
                """{ "productId": "${it.first}", "quantity": ${it.second} }"""
            }

        val supplierId = "supplier1"
        return """
            {
                "requestId": "$requestId",
                "supplierId": "$supplierId",
                "supplierRef": "xxx",
                "deliveredAt": "$deliveredAt",
                "products": [
                    $products
                ]
            }
            """.trimIndent()
    }
}
