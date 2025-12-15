package org.darren.stock.steps

import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.auth.JwtConfig
import org.darren.stock.ktor.idempotency.IdempotencyStore
import org.darren.stock.ktor.idempotency.InMemoryIdempotencyStore
import org.darren.stock.ktor.module
import org.darren.stock.steps.helpers.TestDateTimeProvider
import org.darren.stock.steps.helpers.TestStockEventRepository
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
    fun beforeAllScenarios() =
        runBlocking {
            // Set default System Administrator token for all tests
            // Individual scenarios can override by calling authentication step definitions
            val defaultToken =
                AuthenticationSteps.generateToken(
                    sub = "test-admin",
                    name = "Test Administrator",
                    job = "System Administrator",
                )
            TestContext.setAuthorizationToken(defaultToken)

            testApp = buildKtorTestApp()
            val client = createTestClient(testApp)

            registerTestKoinModules(client)

            testApp.start()
        }

    private fun createTestClient(testApp: TestApplication) =
        testApp.createClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }
        }

    private fun registerTestKoinModules(client: HttpClient) {
        startKoin {
            modules(
                module { single { client } },
                module { single<StockEventRepository> { TestStockEventRepository() } },
                module { single { LocationApiClient(locationHost) } },
                module { single<StockSystem> { StockSystem() } },
                module { single { testApp.client.engine } },
                // Register services introduced by the domain refactor so endpoints can inject them
                module {
                    single<org.darren.stock.domain.service.StockReader> {
                        org.darren.stock.domain.service
                            .StockSystemReader(get())
                    }
                },
                module {
                    single<org.darren.stock.domain.service.LocationValidator> {
                        org.darren.stock.domain.service
                            .LocationApiClientValidator(get())
                    }
                },
                module {
                    single<org.darren.stock.domain.service.StockService> {
                        org.darren.stock.domain.service
                            .StockService(get(), get())
                    }
                },
                module { single<ServiceLifecycleSteps> { this@ServiceLifecycleSteps } },
                module { single { ApiCallStepDefinitions() } },
                module { single { TestDateTimeProvider() } },
                module { single<DateTimeProvider> { get<TestDateTimeProvider>() } },
                module { single<IdempotencyStore> { InMemoryIdempotencyStore() } },
                module {
                    single {
                        JwtConfig(
                            publicKey = AuthenticationSteps.getPublicKey(),
                            issuer = "https://identity-provider.example.com",
                            audience = "stock-api",
                        )
                    }
                },
            )
        }
    }

    private fun buildKtorTestApp(): TestApplication =
        TestApplication {
            application { module() }

            externalServices {
                hosts(locationHost) {
                    routing {
                        get("/locations/{id}") { getLocationByIdResponder(call) }
                        get("/locations/{id}/children") { getChildrenByIdResponder(call) }
                        get("/locations/{id}/path") { getPathResponder(call) }
                    }
                }
            }
        }

    var getLocationByIdResponder: suspend (call: RoutingCall) -> Unit =
        { logger.warn { "getLocationByIdResponder not set" } }

    var getChildrenByIdResponder: suspend (call: RoutingCall) -> Unit =
        { logger.warn { "getChildrenByIdResponder not set" } }

    var getPathResponder: suspend (call: RoutingCall) -> Unit =
        { logger.warn { "getPathResponder not set" } }

    @Given("the service is running")
    fun theServiceIsRunning() =
        runBlocking {
            assertTrue(this@ServiceLifecycleSteps::testApp.isInitialized)
            testApp.start()
        }

    @After
    fun shutdownTestServerAfterScenario() =
        runBlocking {
            if (this@ServiceLifecycleSteps::testApp.isInitialized) {
                testApp.stop()
            }
            stopKoin()
        }
}
