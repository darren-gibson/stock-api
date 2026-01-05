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
import org.darren.stock.domain.service.*
import org.darren.stock.domain.snapshot.EventCountSnapshotStrategyFactory
import org.darren.stock.domain.snapshot.SnapshotRepository
import org.darren.stock.domain.snapshot.SnapshotStrategyFactory
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.persistence.InMemorySnapshotRepository
import org.darren.stock.persistence.InMemoryStockEventRepository
import org.koin.dsl.module
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
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
            single<IdempotencyService> { IdempotencyService(get()) }
        }

    val stockEventRepositoryModule =
        module {
            single<StockEventRepository> { InMemoryStockEventRepository() }
        }

    val snapshotRepositoryModule =
        module {
            single<SnapshotRepository> { InMemorySnapshotRepository() }
        }

    val snapshotStrategyModule =
        module {
            single<SnapshotStrategyFactory> { EventCountSnapshotStrategyFactory(get(), 5) }
        }

    val dateTimeProviderModule =
        module {
            single<DateTimeProvider> {
                object : DateTimeProvider {
                    override fun now() = LocalDateTime.now()
                }
            }
        }

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
