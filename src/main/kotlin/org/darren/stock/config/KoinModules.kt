package org.darren.stock.config

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.idempotency.IdempotencyStore
import org.darren.stock.ktor.idempotency.InMemoryIdempotencyStore
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
            single<IdempotencyStore> { InMemoryIdempotencyStore() }
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
