package org.darren.stock.config

import io.github.smyrgeorge.actor4k.system.ActorSystem
import io.github.smyrgeorge.actor4k.system.registry.SimpleActorRegistry
import io.github.smyrgeorge.actor4k.util.SimpleLoggerFactory
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.Meter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.actors.StockPotActor
import org.darren.stock.domain.service.*
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.idempotency.*
import org.darren.stock.persistence.InMemoryStockEventRepository
import org.koin.dsl.module
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

/**
 * Marker interface for Actor4k initializer.
 */
interface Actor4kInitializer

/**
 * Application-level Koin module definitions.
 * Separated from main() to improve testability and maintainability.
 */
object KoinModules {
    val httpClientModule =
        module {
            single<HttpClientEngine> { CIO.create() }
        }

    val locationApiModule =
        module {
            single { LocationApiClient(getProperty("LOCATION_API")) }
        }

    val stockSystemModule =
        module {
            single<StockSystem> { StockSystem() }
            single<StockReader> { StockSystemReader(get()) }
            single<LocationValidator> { LocationApiClientValidator(get()) }
            single<StockService> { StockService(get(), get()) }
        }

    val stockEventRepositoryModule =
        module {
            single<StockEventRepository> { InMemoryStockEventRepository() }
        }

    val dateTimeProviderModule =
        module {
            single<DateTimeProvider> {
                object : DateTimeProvider {
                    override fun now() = LocalDateTime.now()
                }
            }
        }

    val idempotencyModule =
        module {
            single<IdempotencyStore> {
                val ttl = getProperty("IDEMPOTENCY_TTL_SECONDS", "86400").toLong()
                val max = getProperty("IDEMPOTENCY_MAX_SIZE", "10000").toLong()
                InMemoryIdempotencyStore(ttlSeconds = ttl, maximumSize = max)
            }
            single<RequestFingerprint> {
                DefaultRequestFingerprint()
            }
            single<Meter> {
                GlobalOpenTelemetry.get().getMeter(IdempotencyMetrics.METER_NAME)
            }

            single<ResponseCacher> {
                // Wrap the default cacher with OTEL metrics decorator; meter is injected so tests can
                // provide a local meter when needed.
                OtelResponseCacher(DefaultResponseCacher(get()), get())
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
                                    StockPotActor(key)
                                }
                        ActorSystem
                            .register(loggerFactory)
                            .register(registry)
                            .start()
                    }
                }
            }
        }

    /**
     * Returns all application modules for production use.
     */
    fun allModules() =
        listOf(
            httpClientModule,
            locationApiModule,
            stockSystemModule,
            stockEventRepositoryModule,
            dateTimeProviderModule,
            idempotencyModule,
            actor4kModule,
        )
}
