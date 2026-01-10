package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.statement.*
import kotlinx.coroutines.test.runTest
import org.darren.stock.steps.helpers.TestDateTimeProvider
import org.darren.stock.steps.helpers.getRequiredDouble
import org.darren.stock.steps.helpers.getRequiredString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class StockMovementStepDefinitions : KoinComponent {
    private val apiCallStepDefinitions by inject<ApiCallStepDefinitions>()
    private var lastException: Exception? = null
    lateinit var response: HttpResponse
    private val dateTimeProvider: TestDateTimeProvider by inject()

    @When("I initiate a stock movement transaction with the following details:")
    fun iInitiateAStockMovementTransactionWithTheFollowingDetails(stockMovements: List<StockMovement>) =
        runTest {
            stockMovements.forEach { movement ->
                with(movement) {
                    performStockMove(from, to, product, quantity)
                }
            }
        }

    @Then("the move request should fail with an InsufficientStock exception")
    fun theMoveRequestShouldFailWithAnInsufficientStockException() =
        runTest {
            apiCallStepDefinitions.theAPIShouldRespondWithStatusCode(400)
            apiCallStepDefinitions.theResponseBodyShouldContain("""{"status":"InsufficientStock"}""")
        }

    @DataTableType
    fun locationEntryTransformer(row: Map<String?, String>) =
        StockMovement(
            row.getRequiredString("source"),
            row.getRequiredString("destination"),
            row.getRequiredString("product"),
            row.getRequiredDouble("quantity"),
            row.getRequiredString("reason"),
        )

    @And("{double} {string} is moved from {string} to {string}")
    fun moveFromTo(
        quantity: Double,
        product: String,
        from: String,
        to: String,
    ) = runTest {
        performStockMove(from, to, product, quantity)
    }

    private fun performStockMove(
        from: String,
        to: String,
        product: String,
        quantity: Double,
    ) {
        try {
            val url = "/locations/$from/$product/movements"
            val requestId = UUID.randomUUID().toString()
            val movedAt = dateTimeProvider.nowAsString()
            val payload = """{
                                  "destinationLocationId": "$to",
                                  "quantity": $quantity,
                                  "requestId": "$requestId",
                                  "reason": "replenishment",
                                  "movedAt": "$movedAt"
                              }"""

            apiCallStepDefinitions.iSendAPOSTRequestToWithTheFollowingPayload(url, payload)
        } catch (e: Exception) {
            lastException = e
        }
    }

    data class StockMovement(
        val from: String,
        val to: String,
        val product: String,
        val quantity: Double,
        val reason: String,
    )
}
