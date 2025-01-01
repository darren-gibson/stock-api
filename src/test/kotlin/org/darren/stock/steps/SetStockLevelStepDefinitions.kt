package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import kotlinx.coroutines.runBlocking
import org.darren.stock.steps.helpers.TestDateTimeProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime
import java.util.*

class SetStockLevelStepDefinitions : KoinComponent {
    private val apiCallStepDefinitions by inject<ApiCallStepDefinitions>()
    private val dateTimeProvider: TestDateTimeProvider by inject()

    @Given("a Distribution Centre {string} with {double} units of {string}")
    @Given("a Store {string} with {double} units of {string}")
    fun aStoreWithUnitsOf(locationId: String, quantity: Double, productId: String) = runBlocking {
        theStockLevelOfProductInStoreIs(productId, locationId, quantity)
    }

    @Given("the stock level of {string} in {string} is {double}")
    @Given("a product {string} exists in {string} with a stock level of {double}")
    fun theStockLevelOfProductInStoreIs(productId: String, locationId: String, quantity: Double) = runBlocking {
        val url = "/locations/$locationId/products/$productId/counts"
        val requestId = UUID.randomUUID().toString()
        val countedAt = dateTimeProvider.asString(LocalDateTime.MIN)

        val payload = """{"requestId": "$requestId", "reason": "AdminOverride", "countedAt": "$countedAt", "quantity": $quantity}"""

        apiCallStepDefinitions.sendPostRequest(url, payload)
    }

    @And("the following are the current stock levels:")
    fun theFollowingAreTheCurrentStockLevels(stockLevels: List<StockLevel>) {
        stockLevels.forEach { stockLevel ->
            theStockLevelOfProductInStoreIs(stockLevel.productId, stockLevel.locationId, stockLevel.quantity)
        }
    }

    @DataTableType
    fun stockLevelTransformer(row: Map<String?, String>): StockLevel {
        return StockLevel(row["Location Id"]!!, row["Product"]!!, row["Stock Level"]!!.toDouble())
    }

    data class StockLevel(val locationId: String, val productId: String, val quantity: Double)
}