package org.darren.stock.steps

import org.darren.stock.ktor.module
import io.cucumber.java.PendingException
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking

class PersistenceStepDefinitions {
    private lateinit var testApp: TestApplication

    @Given("the Stock API is connected to a persistent database")
    fun theStockAPIIsConnectedToAPersistentDatabase() = runBlocking {
        testApp = TestApplication {
            application {
                module()
            }
        }
        testApp.start()
    }

    @And("stock data includes SKUs, quantities, and locations")
    fun stockDataIncludesSKUsQuantitiesAndLocations() {
        throw PendingException()
    }

    @When("the server is restarted after a failure")
    fun theServerIsRestartedAfterAFailure() {
        throw PendingException()
    }

    @And("no stock discrepancies are introduced")
    fun noStockDiscrepanciesAreIntroduced() {
        throw PendingException()
    }
}