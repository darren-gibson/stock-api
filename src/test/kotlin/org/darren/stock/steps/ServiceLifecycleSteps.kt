package org.darren.stock.steps

import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.actors.LocationActor.Companion.locationActor
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.module
import org.darren.stock.persistence.InMemoryStockEventRepository
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.ktor.server.testing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ServiceLifecycleSteps {
    private lateinit var testApp: TestApplication

    @OptIn(DelicateCoroutinesApi::class)
    @Before
    fun beforeAllScenarios() = runBlocking {
        testApp = TestApplication { application { module() } }
        startKoin {
            modules(
                module { single { testApp } },
                module { single<StockEventRepository> { InMemoryStockEventRepository() } },
                module { single { GlobalScope.locationActor() } },
                module { single<StockSystem> { StockSystem() } }
            )
        }
        testApp.start()
    }

    @Given("the service is running")
    fun theServiceIsRunning() = runBlocking {
        assertTrue(this@ServiceLifecycleSteps::testApp.isInitialized)
        testApp.start()
    }

    @After
    fun shutdownTestServerAfterScenario() {
        if (this::testApp.isInitialized)
            testApp.stop()
        stopKoin()
    }
}