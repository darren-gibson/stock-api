package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeliveryStepDefinitions : KoinComponent {
    private lateinit var response: HttpResponse
    private val testApp by inject<TestApplication>()


    @Given("{string} is due to be delivered by {string} to {string} with a quantity of {double}")
    fun isDueToBeDeliveredByToWithAQuantityOf(
        productId: String, supplierId: String, locationIs: String, quantity: Double
    ) {
    }

    @Given("{string} is expecting the following deliveries from {string}:")
    fun isExpectingTheFollowingDeliveriesFrom(
        locationId: String, supplierId: String, productQuantities: List<ProductQuantity>
    ) {
    }

    @DataTableType
    fun productQuantityTransformer(row: Map<String?, String>): ProductQuantity {
        return ProductQuantity(row["Product ID"]!!, row["Quantity"]!!.toDouble())
    }

    data class ProductQuantity(val productId: String, val quantity: Double)

    @When("there is a delivery of {double} {string} and {double} {string} to {string}")
    fun thereIsADeliveryOfAndTo(
        quantity1: Double, productId1: String, quantity2: Double, productId2: String, locationId: String
    ) = runBlocking {
        runDeliveryForProducts(locationId, productId1 to quantity1, productId2 to quantity2)
    }

    private suspend fun runDeliveryForProducts(locationId: String, vararg products: Pair<String, Double>) {
        val payload = buildBody(products.toList())

        val client = testApp.createClient { install(ContentNegotiation) { json() } }

        response = client.post("/locations/$locationId/deliveries") {
            setBody(payload)
            contentType(ContentType.Application.Json)
        }
        assertTrue(response.status.isSuccess())
    }

    private fun buildBody(productQuantities: List<Pair<String, Double>>): String {
        val products = productQuantities.joinToString(",") {
            """{ "productId": "${it.first}", "quantity": ${it.second} }"""
        }

        val supplierId = "supplier1"
        return """
            {
                "requestId": "${System.currentTimeMillis()}",
                "supplierId": "$supplierId",
                "supplierRef": "xxx",
                "deliveryDate": "2024-12-26T15:35:00Z",
                "products": [
                    $products
                ]
            }
        """.trimIndent()
    }

    @When("there is a delivery of {double} {string} to {string}")
    fun thereIsADeliveryOfTo(quantity: Double, productId: String, locationId: String) = runBlocking {
        runDeliveryForProducts(locationId, productId to quantity)
    }
}