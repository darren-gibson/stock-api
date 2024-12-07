package com.darren.stock.steps

import com.darren.stock.domain.StockEventRepository
import com.darren.stock.ktor.module
import com.darren.stock.persistence.InMemoryStockEventRepository
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.ktor.server.testing.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import kotlin.test.assertTrue

class ServiceLifecycleSteps {
    private lateinit var testApp: TestApplication

    @Before
    fun beforeAllScenarios() {
        startKoin {
            module {
                single<StockEventRepository> { InMemoryStockEventRepository() }
            }
        }
        testApp = TestApplication {
            application {
                module()
            }
        }
        testApp.start()
        getKoin().loadModules(listOf(module { single { testApp } }))
    }


    @Given("the service is running")
    fun theServiceIsRunning() {
        assertTrue { this::testApp.isInitialized }
        testApp.start()
    }

    @After
    fun shutdownTestServerAfterScenario() {
        if(this::testApp.isInitialized)
            testApp.stop()
        stopKoin()
    }
}