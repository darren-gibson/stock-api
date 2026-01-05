package org.darren.stock.steps

import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.Scenario
import io.cucumber.java.en.Given
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.actor4k.system.ActorSystem
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.darren.stock.config.KoinModules.actor4kModule
import org.darren.stock.config.TestKoinModules
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.ktor.auth.JwtConfig
import org.darren.stock.ktor.module
import org.darren.stock.steps.helpers.ActorSystemTestConfig
import org.darren.stock.steps.helpers.TestDateTimeProvider
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.Closeable
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration.Companion.milliseconds

class ServiceLifecycleSteps : KoinComponent {
    private lateinit var testApp: TestApplication
    private val locationHost = "http://location.api.darren.org"
    private var isTestSuiteInitialized = false

    companion object {
        private val logger = KotlinLogging.logger {}
        private const val MANUAL_SERVICE_START_TAG = "@manual-service-start"

        init {
            // Add shutdown hook to close OpenTelemetry SDK once at end of suite
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    try {
                        val sdk =
                            io.opentelemetry.api.GlobalOpenTelemetry
                                .get()
                        if (sdk is io.opentelemetry.sdk.OpenTelemetrySdk) {
                            sdk.close()
                        }
                    } catch (_: Exception) {
                        // Ignore errors during final shutdown
                    }
                },
            )
        }
    }

    @Before
    fun beforeAllScenarios(scenario: Scenario) =
        runTest {
            // Skip automatic service start for scenarios that want manual control
            if (scenario.sourceTagNames.contains(MANUAL_SERVICE_START_TAG)) {
                logger.info { "===> Skipping automatic service start for ${scenario.name} (has $MANUAL_SERVICE_START_TAG tag) <==" }
                return@runTest
            }

            initializeTestSuiteOnce()
            initializeAndStartServiceOnce()
        }

    private fun initializeTestSuiteOnce() {
        if (!isTestSuiteInitialized) {
            isTestSuiteInitialized = true

            // Initialize test-specific OpenTelemetry SDK (once for entire suite)
            // Use minimal configuration with disabled exporters for fast test execution
            val testOtel =
                io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
                    .builder()
                    .addPropertiesSupplier {
                        mapOf(
                            "otel.service.name" to "Stock API Tests",
                            "otel.traces.exporter" to "none",
                            "otel.metrics.exporter" to "none",
                            "otel.logs.exporter" to "none",
                        )
                    }.build()
                    .openTelemetrySdk
            org.darren.stock.ktor
                .setOpenTelemetryForTests(testOtel)
        }
    }

    private fun initializeAndStartServiceOnce() =
        runTest {
            // Only initialize once per suite, subsequent calls just ensure it's started
            if (this@ServiceLifecycleSteps::testApp.isInitialized) {
                testApp.start()
                return@runTest
            }

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
        logger.info { "===> Registering test Koin modules <==" }
        startKoin {
            // Provide properties used by production modules
            properties(mapOf("LOCATION_API" to locationHost))

            val actorConf =
                ActorSystemTestConfig.applyTo(
                    ActorSystem.Conf(
                        shutdownInitialDelay = 1.milliseconds,
                        shutdownPollingInterval = 1.milliseconds,
                        shutdownFinalDelay = 0.milliseconds,
                    ),
                )

            logger.info { "===> Actor system configuration: expiresAfter=${actorConf.actorExpiresAfter}, cleanupEvery=${actorConf.registryCleanupEvery} <==" }

            // Load only test modules to provide test-scoped bindings (avoid duplicate production bindings)
            modules(
                TestKoinModules.testModules(client) +
                    listOf(
                        // Test-specific small modules that don't belong in a shared test helper
                        module { single { testApp.client.engine } },
                        actor4kModule(actorConf),
                        module { single<ServiceLifecycleSteps> { this@ServiceLifecycleSteps } },
                        module { single { ApiCallStepDefinitions() } },
                        module { single { ObservabilityLoggingStepDefinitions() } },
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
            application {
                module()
                intercept(io.ktor.server.application.ApplicationCallPipeline.Call) {
                    val span = Span.current()

                    if (span.spanContext.isValid) {
                        val traceId = span.spanContext.traceId
                        val spanId = span.spanContext.spanId
                        logger.debug { "Trace from OpenTelemetry traceId=$traceId, spanId=$spanId" }
                    } else {
                        logger.warn { "OpenTelemetry span is not valid" }
                    }
                }
            }

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
        runTest {
            initializeTestSuiteOnce()
            initializeAndStartServiceOnce()
        }

    @After
    fun shutdownTestServerAfterScenario() =
        runTest {
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
            ActorSystemTestConfig.clearOverrides()

            // TODO: Consider whether ActorSystem should be a Koin-managed singleton instead
            ActorSystem.shutdown()
        }

    @Given("the application is running")
    fun theApplicationIsRunning() {
        // For documentation purposes only
    }
}
