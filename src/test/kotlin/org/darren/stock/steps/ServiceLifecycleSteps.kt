package org.darren.stock.steps

import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.actor4k.system.ActorSystem
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.darren.stock.config.KoinModules.actor4kModule
import org.darren.stock.config.TestKoinModules
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.ktor.auth.JwtConfig
import org.darren.stock.ktor.module
import org.darren.stock.steps.helpers.TestDateTimeProvider
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.Closeable
import kotlin.coroutines.ContinuationInterceptor

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
            // Provide properties used by production modules
            properties(mapOf("LOCATION_API" to locationHost))

            // Load only test modules to provide test-scoped bindings (avoid duplicate production bindings)
            modules(
                TestKoinModules.testModules(client) +
                    listOf(
                        // Test-specific small modules that don't belong in a shared test helper
                        module { single { testApp.client.engine } },
                        actor4kModule,
                        module { single<ServiceLifecycleSteps> { this@ServiceLifecycleSteps } },
                        module { single { ApiCallStepDefinitions() } },
                        module { single { TestDateTimeProvider() } },
                        module { single<DateTimeProvider> { get<TestDateTimeProvider>() } },
                        // JwtConfig remains test-scoped for scenarios
                        module {
                            single {
                                JwtConfig(
                                    publicKey = AuthenticationSteps.getPublicKey(),
                                    issuer = "https://identity-provider.example.com",
                                    audience = "stock-api",
                                )
                            }
                        },
                    ),
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
            // Attempt to cancel and close any test-specific coroutine scope (created by TestKoinModules)
            try {
                val koin = getKoin()
                val scope = koin.getOrNull<CoroutineScope>()
                if (scope != null) {
                    scope.cancel()
                    val dispatcher = scope.coroutineContext[ContinuationInterceptor] as? Closeable
                    dispatcher?.close()
                }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to cleanup test coroutine scope" }
            }

            stopKoin()
            // TODO: Consider whether ActorSystem should be a Koin-managed singleton instead
            ActorSystem.shutdown()
        }
}
