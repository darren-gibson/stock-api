package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import kotlinx.coroutines.runBlocking
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.actors.events.OverrideStockLevelEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime

class SetStockLevelStepDefinitions : KoinComponent {
    @Given("a Distribution Centre {string} with {double} units of {string}")
    @Given("a Store {string} with {double} units of {string}")
    fun aStoreWithUnitsOf(
        locationId: String,
        quantity: Double,
        productId: String,
    ) = runBlocking {
        theStockLevelOfProductInStoreIs(productId, locationId, quantity)
    }

    @Given("the stock level of {string} in {string} is {double}")
    @Given("a product {string} exists in {string} with a stock level of {double}")
    fun theStockLevelOfProductInStoreIs(
        productId: String,
        locationId: String,
        quantity: Double,
    ) = runBlocking {
        setStockLevels(productId, locationId, quantity)
    }

    @Given("{string} is a valid product")
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun isAValidProduct(productId: String) {
        // No-op: product existence is not validated in this system
        // Products are considered valid by virtue of being referenced
    }

    private fun setStockLevels(
        productId: String,
        locationId: String,
        quantity: Double,
        pendingAdjustment: Double = 0.0,
    ) = runBlocking {
        val repo: StockEventRepository by inject()
        val overrideAsAt = LocalDateTime.MIN

        repo.insert(locationId, productId, OverrideStockLevelEvent(quantity, pendingAdjustment, overrideAsAt))
    }

    @And("the following are the current stock levels:")
    fun theFollowingAreTheCurrentStockLevels(stockLevels: List<StockLevel>) =
        runBlocking {
            stockLevels.forEach { stockLevel ->
                setStockLevels(stockLevel.productId, stockLevel.locationId, stockLevel.quantity, stockLevel.pendingAdjustment)
            }
        }

    @DataTableType
    fun stockLevelTransformer(row: Map<String?, String>): StockLevel =
        StockLevel(
            row["Location Id"]!!,
            row["Product"]!!,
            row["Stock Level"]!!.toDouble(),
            row["Pending Adjustment"]?.toDouble() ?: 0.0,
        )

    data class StockLevel(
        val locationId: String,
        val productId: String,
        val quantity: Double,
        val pendingAdjustment: Double,
    )
}
