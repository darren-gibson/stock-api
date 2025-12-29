package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.steps.helpers.TestDateTimeProvider
import org.darren.stock.steps.helpers.TestStockEventRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime

class DeliveryStepDefinitions : KoinComponent {
    private val logger = KotlinLogging.logger {}
    private lateinit var response: Any
    private val repository: StockEventRepository by inject()
    private val client: HttpClient by inject()
    private val dateTimeProvider: TestDateTimeProvider by inject()

    @Given("{string} is due to be delivered by {string} to {string} with a quantity of {double}")
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun isDueToBeDeliveredByToWithAQuantityOf(
        productId: String,
        supplierId: String,
        locationIs: String,
        quantity: Double,
    ) {
        // Placeholder: Future implementation will track expected deliveries
    }

    @Given("{string} is expecting the following deliveries from {string}:")
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun isExpectingTheFollowingDeliveriesFrom(
        locationId: String,
        supplierId: String,
        productQuantities: List<ProductQuantity>,
    ) {
        // Placeholder: Future implementation will track expected deliveries by supplier
    }

    @DataTableType
    fun productQuantityTransformer(row: Map<String?, String>): ProductQuantity = ProductQuantity(row["Product ID"]!!, row["Quantity"]!!.toDouble())

    data class ProductQuantity(
        val productId: String,
        val quantity: Double,
    )

    @When("there is a delivery of {double} {string} and {double} {string} to {string}")
    fun thereIsADeliveryOfAndTo(
        quantity1: Double,
        productId1: String,
        quantity2: Double,
        productId2: String,
        locationId: String,
    ) = runBlocking {
        runDeliveryForProducts(locationId, productId1 to quantity1, productId2 to quantity2)
    }

    private suspend fun runDeliveryForProducts(
        locationId: String,
        vararg products: Pair<String, Double>,
        deliveredAt: String = dateTimeProvider.nowAsString(),
    ) {
        val payload = buildBody(products.toList(), deliveredAt)

        response =
            client.post("/locations/$locationId/deliveries") {
                setBody(payload)
                contentType(ContentType.Application.Json)
                TestContext.getAuthorizationToken()?.let { token ->
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
            }
        val resp = response
        assertTrue((resp as HttpResponse).status.isSuccess())
    }

    private fun buildBody(
        productQuantities: List<Pair<String, Double>>,
        deliveredAt: String,
    ): String {
        val products =
            productQuantities.joinToString(",") {
                """{ "productId": "${it.first}", "quantity": ${it.second} }"""
            }

        val supplierId = "supplier1"
        return """
            {
                "requestId": "${System.currentTimeMillis()}",
                "supplierId": "$supplierId",
                "supplierRef": "xxx",
                "deliveredAt": "$deliveredAt",
                "products": [
                    $products
                ]
            }
            """.trimIndent()
    }

    @When("there is a delivery of {double} {string} to {string}")
    @When("there is a delivery of {quantity} of {string} to {string}")
    fun thereIsADeliveryOfTo(
        quantity: Double,
        productId: String,
        locationId: String,
    ) = runBlocking {
        runDeliveryForProducts(locationId, productId to quantity)
    }

    @And("a delivery of {quantity} of {string} to the {string} store that occurred at {dateTime}")
    fun aDeliveryOfCansOfToTheStoreThatOccurredAtOn(
        quantity: Double,
        productId: String,
        locationId: String,
        dateTime: LocalDateTime,
    ) = runBlocking {
        runDeliveryForProducts(locationId, productId to quantity, deliveredAt = dateTimeProvider.asString(dateTime))
    }

    @When("I attempt a delivery of {double} {string} and {double} {string} to {string}")
    fun iAttemptADeliveryOfAndTo(
        quantity1: Double,
        productId1: String,
        quantity2: Double,
        productId2: String,
        locationId: String,
    ) = runBlocking {
        runDeliveryAttemptForProducts(locationId, productId1 to quantity1, productId2 to quantity2)
    }

    private suspend fun runDeliveryAttemptForProducts(
        locationId: String,
        vararg products: Pair<String, Double>,
        deliveredAt: String = dateTimeProvider.nowAsString(),
    ) {
        val payload = buildBody(products.toList(), deliveredAt)

        try {
            response =
                client.post("/locations/$locationId/deliveries") {
                    setBody(payload)
                    contentType(ContentType.Application.Json)
                    TestContext.getAuthorizationToken()?.let { token ->
                        headers {
                            append(HttpHeaders.Authorization, "Bearer $token")
                        }
                    }
                }
        } catch (e: Exception) {
            logger.debug(e) { "Delivery request failed as expected" }
            response = "failed"
        }
        // Do not assert success, as this is for failed deliveries
    }

    @Given("the repository will fail inserts for {string} up to {int} times")
    fun theRepositoryWillFailInsertsFor(
        productId: String,
        maxFailures: Int,
    ) {
        (repository as? TestStockEventRepository)?.failInsertsForProducts(setOf(productId), maxFailures)
    }

    @Then("the delivery should succeed")
    fun theDeliveryShouldSucceed() =
        runBlocking {
            val resp = response as HttpResponse
            assertTrue(resp.status.isSuccess(), "Expected delivery to succeed but it failed with status: ${resp.status}. Response: ${resp.bodyAsText()}")
        }

    @Then("the delivery should fail")
    fun theDeliveryShouldFail() {
        val resp = response
        val failed =
            if (resp is HttpResponse) {
                !resp.status.isSuccess()
            } else if (resp == "failed") {
                true
            } else {
                false
            }
        assertTrue(failed)
    }
}
