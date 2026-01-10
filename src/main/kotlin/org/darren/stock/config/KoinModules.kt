package org.darren.stock.config

import io.github.smyrgeorge.actor4k.system.ActorSystem
import io.github.smyrgeorge.actor4k.system.registry.SimpleActorRegistry
import io.github.smyrgeorge.actor4k.util.SimpleLoggerFactory
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.actors.IdempotencyService
import org.darren.stock.domain.actors.StockPotActor
import org.darren.stock.domain.resilience.ApiResilienceConfig
import org.darren.stock.domain.resilience.ApiResilienceManager
import org.darren.stock.domain.resilience.BackoffConfig
import org.darren.stock.domain.resilience.FailFastConfig
import org.darren.stock.domain.service.*
import org.darren.stock.domain.snapshot.EventCountSnapshotStrategyFactory
import org.darren.stock.domain.snapshot.SnapshotRepository
import org.darren.stock.domain.snapshot.SnapshotStrategyFactory
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.persistence.InMemorySnapshotRepository
import org.darren.stock.persistence.InMemoryStockEventRepository
import org.koin.dsl.module
import java.time.LocalDateTime
import kotlin.time.Duration
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
    private val httpClientModule =
        module {
            single<HttpClientEngine> { CIO.create() }
        }

    private val locationApiModule =
        module {
            single<ApiResilienceManager> {
                ApiResilienceManager(
                    ApiResilienceConfig(
                        backoff =
                            BackoffConfig(
                                maxAttempts = getProperty("resilience.apis.location.backoff.maxAttempts", "3").toInt(),
                                initialDelay = Duration.parse(getProperty("resilience.apis.location.backoff.initialDelay", "100ms")),
                                maxDelay = Duration.parse(getProperty("resilience.apis.location.backoff.maxDelay", "2s")),
                                multiplier =
                                    getProperty("resilience.apis.location.backoff.multiplier", "2.0")
                                        .toDouble(),
                            ),
                        failFast =
                            FailFastConfig(
                                failureThreshold =
                                    getProperty("resilience.apis.location.failFast.failureThreshold", "5")
                                        .toInt(),
                                quietPeriod =
                                    Duration.parse(
                                        getProperty("resilience.apis.location.failFast.quietPeriod", "60s"),
                                    ),
                                testAfterQuietPeriod =
                                    getProperty("resilience.apis.location.failFast.testAfterQuietPeriod", "true")
                                        .toBoolean(),
                            ),
                    ),
                )
            }
            single<LocationApiClient> { LocationApiClient(getProperty("LOCATION_API"), get(), get()) }
        }

    private val stockSystemModule =
        module {
            single<StockSystem> { StockSystem() }
            single<StockReader> { StockSystemReader(get()) }
            single<LocationValidator> { LocationApiClientValidator(get()) }
            single<StockService> { StockService(get(), get()) }
            single<IdempotencyService> { IdempotencyService(get()) }
        }

    private val stockEventRepositoryModule =
        module {
            single<StockEventRepository> { InMemoryStockEventRepository() }
        }

    private val snapshotRepositoryModule =
        module {
            single<SnapshotRepository> { InMemorySnapshotRepository() }
        }

    private val snapshotStrategyModule =
        module {
            single<SnapshotStrategyFactory> { EventCountSnapshotStrategyFactory(get(), 5) }
        }

    private val dateTimeProviderModule =
        module {
            single<DateTimeProvider> {
                object : DateTimeProvider {
                    override fun now() = LocalDateTime.now()
                }
            }
        }

    /**
     * Initializes the Actor4k system with structured concurrency management.
     *
     * ## Initialization Sequence
     *
     * 1. Shutdown any previous ActorSystem instance (for test isolation)
     * 2. Configure actor lifecycle parameters (expiry, cleanup intervals)
     * 3. Register the factory for creating [StockPotActor] instances
     * 4. Start the ActorSystem (begins background cleanup tasks)
     *
     * ## Lifecycle Management
     *
     * The ActorSystem manages its own CoroutineScope internally for:
     * - Processing actor messages sequentially
     * - Running background cleanup tasks
     * - Coordinating shutdown
     *
     * Graceful shutdown is triggered via [ActorSystem.shutdown()], typically called during
     * application shutdown or between tests via Koin cleanup.
     *
     * ## Memory Management
     *
     * Inactive actors are automatically evicted based on configuration:
     * - `actorExpiresAfter`: How long before removing inactive actors from memory
     * - `registryCleanupEvery`: How often the cleanup task runs
     *
     * Evicted actors are recreated on next access with state restored from event history.
     *
     * @param conf ActorSystem configuration (expiry, cleanup intervals, etc.)
     */
    fun actor4kModule(conf: ActorSystem.Conf = ActorSystem.Conf()) =
        module(createdAtStart = true) {
            single<Actor4kInitializer> {
                object : Actor4kInitializer {
                    init {
                        // Ensure any previous system is shut down
                        runBlocking {
                            withTimeout(2.seconds) { ActorSystem.shutdown() }
                        }
                        ActorSystem.conf(conf) // need to configure before the registry is created
                        val loggerFactory = SimpleLoggerFactory()
                        val registry =
                            SimpleActorRegistry(loggerFactory)
                                .factoryFor(StockPotActor::class) { key ->
                                    StockPotActor(key, get(), get())
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
            snapshotRepositoryModule,
            snapshotStrategyModule,
            dateTimeProviderModule,
            actor4kModule(),
        )
}
