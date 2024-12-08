package com.darren.stock.steps

import com.darren.stock.domain.LocationMessages
import com.darren.stock.domain.StockEventRepository
import com.darren.stock.domain.StockSystem
import com.darren.stock.domain.actors.LocationActor.Companion.locationActor
import com.darren.stock.ktor.module
import com.darren.stock.persistence.InMemoryStockEventRepository
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.ktor.server.testing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin

class ServiceLifecycleSteps {
    private lateinit var testApp: TestApplication

    @OptIn(DelicateCoroutinesApi::class)
    @Before
    fun beforeAllScenarios() {
        testApp = TestApplication { application { module() } }
        startKoin {
            modules(
                module { single { testApp } },
                module { single<StockEventRepository> { InMemoryStockEventRepository() } },
                module { single<SendChannel<LocationMessages>> { GlobalScope.locationActor() } },
                module { single<StockSystem> { StockSystem() } }
            )
        }
        testApp.start()
    }

    @Given("the service is running")
    fun theServiceIsRunning() {
        assertTrue(this::testApp.isInitialized)
        testApp.start()
    }

    @After
    fun shutdownTestServerAfterScenario() {
        if (this::testApp.isInitialized)
            testApp.stop()
        stopKoin()
    }
}