package org.darren.stock.steps

import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.actor4k.system.ActorSystem
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.darren.stock.steps.helpers.ActorSystemTestConfig
import org.darren.stock.steps.helpers.ActorSystemTestConfig.Overrides
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class StockPotMemoryManagementStepDefinitions {
    private val logger = KotlinLogging.logger {}

    @After(order = 100)
    fun clearMemoryOverrides() {
        ActorSystemTestConfig.clearOverrides()
    }

    @When("I wait for {int} milliseconds")
    fun iWaitForMilliseconds(durationMs: Int) =
        runBlocking {
            logger.info { "Waiting for $durationMs milliseconds" }
            delay(durationMs.toLong())
        }

    @Given("the system is configured with the following StockPot unloading settings:")
    fun theSystemIsConfiguredWithStockPotUnloadingSettings(settings: Map<String, String>) {
        val unloadAfterInactivity = settings["stockPot.unloadAfterInactivity"] ?: "5m"
        val unloadCheckInterval = settings["stockPot.unloadCheckInterval"] ?: "30s"

        logger.info { "===> Configuring StockPot unloading: unloadAfterInactivity=$unloadAfterInactivity, unloadCheckInterval=$unloadCheckInterval <==" }
        ActorSystemTestConfig.setOverrides(
            Overrides(
                actorExpiresAfter = Duration.parse(unloadAfterInactivity),
                registryCleanupEvery = Duration.parse(unloadCheckInterval),
            ),
        )
    }

    @Then("the StockPot for {string} at {string} should remain in memory")
    fun theStockPotShouldRemainInMemory(
        product: String,
        location: String,
    ) = runBlocking {
        val activeActors = ActorSystem.registry.size()
        logger.info { "Checking StockPot for '$product' at '$location' remains in memory: $activeActors active actors" }
        assertEquals(1, activeActors, "Expected StockPot for '$product' at '$location' to remain in memory")
    }

    @Then("the StockPot for {string} at {string} should still be in memory")
    fun theStockPotShouldStillBeInMemory(
        product: String,
        location: String,
    ) = theStockPotShouldRemainInMemory(product, location)

    @Then("the StockPot for {string} at {string} should eventually be removed from memory")
    fun theStockPotShouldEventuallyBeRemovedFromMemory(
        product: String,
        location: String,
    ) = runBlocking {
        withTimeout(5.seconds) {
            var attempts = 0
            while (true) {
                val activeActors = ActorSystem.registry.size()
                logger.debug { "Polling attempt $attempts: $activeActors active actors (checking if StockPot removed)" }
                if (activeActors == 0) {
                    logger.info { "StockPot for '$product' at '$location' successfully removed from memory after $attempts attempts" }
                    return@withTimeout
                }
                attempts++
                if (attempts > 240) {
                    logger.error { "Giving up after $attempts attempts, still have $activeActors active actors" }
                    throw RuntimeException("StockPot for '$product' at '$location' was not removed from memory after ${attempts * 25}ms")
                }
                delay(25.milliseconds)
            }
        }
    }
}
