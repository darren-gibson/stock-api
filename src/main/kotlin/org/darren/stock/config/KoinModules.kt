package org.darren.stock.config

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.Meter
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.idempotency.DefaultResponseCacher
import org.darren.stock.ktor.idempotency.IdempotencyMetrics
import org.darren.stock.ktor.idempotency.IdempotencyStore
import org.darren.stock.ktor.idempotency.InMemoryIdempotencyStore
import org.darren.stock.ktor.idempotency.OtelResponseCacher
import org.darren.stock.persistence.InMemoryStockEventRepository
import org.koin.dsl.module
import java.time.LocalDateTime

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
            single<org.darren.stock.ktor.idempotency.RequestFingerprint> {
                org.darren.stock.ktor.idempotency
                    .DefaultRequestFingerprint()
            }
            single<Meter> {
                GlobalOpenTelemetry.get().getMeter(IdempotencyMetrics.METER_NAME)
            }

            single<org.darren.stock.ktor.idempotency.ResponseCacher> {
                // Wrap the default cacher with OTEL metrics decorator; meter is injected so tests can
                // provide a local meter when needed.
                OtelResponseCacher(DefaultResponseCacher(get()), get())
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
        )
}
