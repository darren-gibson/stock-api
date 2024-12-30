package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.darren.stock.domain.InsufficientStockException
import org.darren.stock.domain.StockMovement
import org.darren.stock.domain.StockMovementReason
import org.junit.jupiter.api.assertInstanceOf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import io.cucumber.java.en.And

class StockMovementStepDefinitions : KoinComponent {
    private val apiCallStepDefinitions by inject<ApiCallStepDefinitions>()
    private var lastException: Exception? = null
    lateinit var response: HttpResponse

    @When("I initiate a stock movement transaction with the following details:")
    fun iInitiateAStockMovementTransactionWithTheFollowingDetails(stockMovements: List<StockMovement>) =
        runBlocking {

            stockMovements.forEach { movement ->
                with(movement) {
                    performStockMove(from, to, product, quantity)
                }
            }
        }

    @Then("the move request should fail with an InsufficientStock exception")
    fun theMoveRequestShouldFailWithAnInsufficientStockException() {
        assertInstanceOf<InsufficientStockException>(lastException)
    }

    @DataTableType
    fun locationEntryTransformer(row: Map<String?, String>) =
        StockMovement(
            row["source"]!!, row["destination"]!!, row["product"]!!,
            row["quantity"]!!.toDouble(), StockMovementReason.valueOf(row["reason"]!!)
        )

    @And("{double} {string} is moved from {string} to {string}")
    fun moveFromTo(quantity: Double, product: String, from: String, to: String) = runBlocking {
        performStockMove(from, to, product, quantity)
    }

    private suspend fun performStockMove(from: String, to: String, product: String, quantity: Double) {
        try {
            val url = "/locations/$from/$product/movements"
            val requestId = UUID.randomUUID().toString()
            val payload = """{
                                  "destinationLocationId": "$to",
                                  "quantity": $quantity,
                                  "requestId": "$requestId",
                                  "reason": "replenishment"
                              }"""

            response = apiCallStepDefinitions.sendPostRequest(url, payload)
        } catch (e: Exception) {
            lastException = e
        }
    }
}