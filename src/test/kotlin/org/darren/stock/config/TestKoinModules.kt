package org.darren.stock.config

import io.github.smyrgeorge.actor4k.system.ActorSystem
import io.github.smyrgeorge.actor4k.system.registry.SimpleActorRegistry
import io.github.smyrgeorge.actor4k.util.SimpleLoggerFactory
import io.jsonwebtoken.Jwts
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.darren.stock.config.Actor4kInitializer
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.actors.StockPotActor
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.auth.JwtConfig
import org.darren.stock.steps.helpers.TestStockEventRepository
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

/**
 * Test-specific Koin modules that override production bindings used in tests.
 * Keep overrides minimal and explicit so tests only change what they need.
 */
object TestKoinModules {
    // Shared key pair for test JWT tokens
    val testKeyPair =
        Jwts.SIG.RS256
            .keyPair()
            .build()

    fun testModule(
        client: HttpClient,
        stockEventRepository: org.darren.stock.domain.StockEventRepository = TestStockEventRepository(),
    ) = module {
        // Override the HTTP client used by production code
        single<HttpClient> { client }

        // Provide a mock HTTP client engine for LocationApiClient
        single<io.ktor.client.engine.HttpClientEngine> {
            MockEngine { request ->
                // Extract location ID from the request URL
                val url = request.url.toString()
                val locationId = url.substringAfter("/locations/").substringBefore("/")

                // Mock successful responses for location validation
                respond(
                    content = """{"id": "$locationId", "roles": ["TrackedInventoryLocation"], "children": []}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }

        // Use the provided test repository (defaults to TestStockEventRepository)
        single<org.darren.stock.domain.StockEventRepository> { stockEventRepository }

        // Provide a deterministic location API host for tests
        single { LocationApiClient("http://location.api.darren.org") }

        // Stock system used by domain services
        single<StockSystem> {
            StockSystem()
        }

        // JwtConfig for authentication
        single {
            JwtConfig(
                publicKey = testKeyPair.public,
                issuer = "https://identity-provider.example.com",
                audience = "stock-api",
            )
        }

        // DateTimeProvider for domain services
        single<DateTimeProvider> {
            object : DateTimeProvider {
                override fun now() = java.time.LocalDateTime.now()
            }
        }

        // Re-register domain adapters/services so endpoints can resolve them in tests
        single<org.darren.stock.domain.service.StockReader> {
            org.darren.stock.domain.service
                .StockSystemReader(get())
        }
        single<org.darren.stock.domain.service.LocationValidator> {
            org.darren.stock.domain.service
                .LocationApiClientValidator(get())
        }
        single<org.darren.stock.domain.service.StockService> {
            org.darren.stock.domain.service
                .StockService(get(), get())
        }
    }

    val actor4kModule =
        module(createdAtStart = true) {
            single<Actor4kInitializer> {
                object : Actor4kInitializer {
                    init {
                        // Ensure any previous system is shut down
                        runBlocking {
                            withTimeout(2.seconds) { ActorSystem.shutdown() }
                        }
                        val loggerFactory = SimpleLoggerFactory()
                        val registry =
                            SimpleActorRegistry(loggerFactory)
                                .factoryFor(StockPotActor::class) { key ->
                                    StockPotActor(key, get())
                                }
                        ActorSystem
                            .register(loggerFactory)
                            .register(registry)
                            .start()
                    }
                }
            }
        }

    fun testModules(testClient: HttpClient) =
        listOf(
            module {
                // Override the HTTP client used by production code
                single<HttpClient> { testClient }

                // Use a lightweight in-memory test repository
                single<org.darren.stock.domain.StockEventRepository> { TestStockEventRepository() }

                // Provide a deterministic location API host for tests
                single { LocationApiClient("http://location.api.darren.org") }

                // Stock system used by domain services
                single<StockSystem> {
                    StockSystem()
                }

                // DateTimeProvider for domain services
                single<DateTimeProvider> {
                    object : DateTimeProvider {
                        override fun now() = java.time.LocalDateTime.now()
                    }
                }

                // Re-register domain adapters/services so endpoints can resolve them in tests
                single<org.darren.stock.domain.service.StockReader> {
                    org.darren.stock.domain.service
                        .StockSystemReader(get())
                }
                single<org.darren.stock.domain.service.LocationValidator> {
                    org.darren.stock.domain.service
                        .LocationApiClientValidator(get())
                }
                single<org.darren.stock.domain.service.StockService> {
                    org.darren.stock.domain.service
                        .StockService(get(), get())
                }
            },
        )
}
