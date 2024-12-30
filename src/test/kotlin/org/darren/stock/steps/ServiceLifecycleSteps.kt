package org.darren.stock.steps

import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.module
import org.darren.stock.persistence.InMemoryStockEventRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ServiceLifecycleSteps : KoinComponent {
    private lateinit var testApp: TestApplication
    private val locationHost = "http://location.api.darren.org"

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @Before
    fun beforeAllScenarios() = runBlocking {
        testApp = buildKtorTestApp()
        val client = testApp.createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        startKoin {
            modules(
                module { single { client } },
                module { single<StockEventRepository> { InMemoryStockEventRepository() } },
                module { single { LocationApiClient(locationHost) } },
                module { single<StockSystem> { StockSystem() } },
                module { single { testApp.client.engine } },
                module { single<ServiceLifecycleSteps> { this@ServiceLifecycleSteps } },
                module { single { ApiCallStepDefinitions() } }
            )
        }
        testApp.start()
    }

    private fun buildKtorTestApp(): TestApplication {
        return TestApplication {
            application { module() }

            externalServices {
                hosts(locationHost) {
                    routing {
                        get("/locations/{id}") {
                            getLocationByIdResponder(call)
                        }
                        get("/locations/{id}/children") {
                            getChildrenByIdResponder(call)
                        }
                    }
                }
            }
        }
    }

    var getLocationByIdResponder: suspend (call: RoutingCall) -> Unit =
        { logger.warn { "getLocationByIdResponder not set" } }

    var getChildrenByIdResponder: suspend (call: RoutingCall) -> Unit =
        { logger.warn { "getChildrenByIdResponder not set" } }


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