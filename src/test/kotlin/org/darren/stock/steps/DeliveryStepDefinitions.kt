package org.darren.stock.steps

import io.cucumber.java.DataTableType
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.steps.helpers.DeliveryRequestHelper
import org.darren.stock.steps.helpers.TestDateTimeProvider
import org.darren.stock.steps.helpers.TestStockEventRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime

class DeliveryStepDefinitions : KoinComponent {
    private lateinit var response: Any
    private val repository: StockEventRepository by inject()
    private val dateTimeProvider: TestDateTimeProvider by inject()
    private val deliveryRequestHelper: DeliveryRequestHelper by inject()

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
        response =
            deliveryRequestHelper.runDeliveryForProducts(locationId, productId1 to quantity1, productId2 to quantity2)
    }

    @When("there is a delivery of {double} {string} to {string}")
    @When("there is a delivery of {quantity} of {string} to {string}")
    fun thereIsADeliveryOfTo(
        quantity: Double,
        productId: String,
        locationId: String,
    ) = runBlocking {
        response = deliveryRequestHelper.runDeliveryForProducts(locationId, productId to quantity)
    }

    @And("a delivery of {quantity} of {string} to the {string} store that occurred at {dateTime}")
    fun aDeliveryOfCansOfToTheStoreThatOccurredAtOn(
        quantity: Double,
        productId: String,
        locationId: String,
        dateTime: LocalDateTime,
    ) = runBlocking {
        response =
            deliveryRequestHelper.runDeliveryForProducts(
                locationId,
                productId to quantity,
                deliveredAt = dateTimeProvider.asString(dateTime),
            )
    }

    @When("I attempt a delivery of {double} {string} and {double} {string} to {string}")
    fun iAttemptADeliveryOfAndTo(
        quantity1: Double,
        productId1: String,
        quantity2: Double,
        productId2: String,
        locationId: String,
    ) = runBlocking {
        response =
            deliveryRequestHelper.runDeliveryAttemptForProducts(
                locationId,
                productId1 to quantity1,
                productId2 to quantity2,
            )
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
            assertTrue(
                resp.status.isSuccess(),
                "Expected delivery to succeed but it failed with status: ${resp.status}. Response: ${resp.bodyAsText()}",
            )
        }

    @Then("the delivery should fail")
    fun theDeliveryShouldFail() {
        val resp = response
        val failed =
            when (resp) {
                is HttpResponse -> {
                    !resp.status.isSuccess()
                }

                "failed" -> {
                    true
                }

                else -> {
                    false
                }
            }
        assertTrue(failed, "Expected delivery to fail but it succeeded with $resp.")
    }
}
