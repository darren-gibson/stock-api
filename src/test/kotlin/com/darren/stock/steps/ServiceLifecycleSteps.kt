package com.darren.stock.steps

import com.darren.stock.domain.StockEventRepository
import com.darren.stock.domain.StockSystem
import com.darren.stock.domain.actors.LocationActor.Companion.locationActor
import com.darren.stock.domain.handlers.*
import com.darren.stock.ktor.module
import com.darren.stock.persistence.InMemoryStockEventRepository
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.ktor.server.testing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

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
                module { single { GlobalScope.locationActor() } },
                module { single { HandlerHelper() } },
                module { single { SaleHandler(get()) } },
                module { single { CountHandler(get()) } },
                module { single { DeliveryHandler(get()) } },
                module { single { GetValueHandler(get()) } },
                module { single { MoveHandler(get()) } },
                module { single<StockSystem> { StockSystem(get()) } }
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