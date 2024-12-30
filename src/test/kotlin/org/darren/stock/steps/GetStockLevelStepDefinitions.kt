package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.darren.stock.ktor.DateSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime

class GetStockLevelStepDefinitions : KoinComponent {
    private lateinit var response: HttpResponse

    @Then("the current stock level of {string} in {string} will equal {double}")
    @Then("the stock level of {string} in {string} should be updated to {double}")
    fun theCurrentStockLevelOfProductInStoreWillEqual(productId: String, locationId: String, expectedQuantity: Double) =
        runBlocking {
            val actualStock = getStockLevel(locationId, productId)
            assertEquals(expectedQuantity, actualStock)
        }

    private suspend fun getStockLevel(locationId: String, productId: String): Double {
        val stock = getStock(locationId, productId)
        return stock.quantity
    }

    private suspend fun getStock(locationId: String, productId: String): GetStock {
        response = getStockLevelFromApi(locationId, productId)
        assertEquals(HttpStatusCode.OK, response.status)

        return response.body<GetStock>()
    }

    @Serializable
    private data class GetStock(
        val locationId: String,
        val productId: String,
        val quantity: Double,
        @Serializable(with = DateSerializer::class) val lastUpdated: LocalDateTime
    )

    private fun getStockLevelFromApi(locationId: String, productId: String): HttpResponse =
        runBlocking {
            val client: HttpClient by inject()
            val url = "/locations/$locationId/products/$productId"
            response = client.get(url)
            return@runBlocking response
        }

    @Then("the stock levels should be updated as follows:")
    @And("the stock levels should remain unchanged:")
    fun theStockLevelsShouldBeUpdatedAsFollows(expectedStockLevels: List<ExpectedStock>) = runBlocking {
        expectedStockLevels.forEach { expected ->
            val actual = getStockLevel(expected.location, expected.product)
            assertEquals(expected.quantity, actual)
        }
    }

    data class ExpectedStock(val location: String, val product: String, val quantity: Double)

    @DataTableType
    fun expectedStockTransformer(row: Map<String?, String>) =
        ExpectedStock(row["location"]!!, row["product"]!!, row["quantity"]!!.toDouble())
}