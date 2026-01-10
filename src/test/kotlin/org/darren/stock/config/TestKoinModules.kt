package org.darren.stock.config

import io.ktor.client.*
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.resilience.ApiResilienceManager
import org.darren.stock.domain.service.*
import org.darren.stock.domain.snapshot.EventCountSnapshotStrategyFactory
import org.darren.stock.domain.snapshot.SnapshotRepository
import org.darren.stock.domain.snapshot.SnapshotStrategyFactory
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.steps.helpers.DeliveryRequestHelper
import org.darren.stock.steps.helpers.SaleRequestHelper
import org.darren.stock.steps.helpers.TestSnapshotRepository
import org.darren.stock.steps.helpers.TestStockEventRepository
import org.koin.dsl.module
import java.time.LocalDateTime

/**
 * Test-specific Koin modules that override production bindings used in tests.
 * Keep overrides minimal and explicit so tests only change what they need.
 */
object TestKoinModules {
    fun testModules(testClient: HttpClient) =
        listOf(
            module {
                // Override the HTTP client used by production code
                single<HttpClient> { testClient }

                // Use a lightweight in-memory test repository
                single<StockEventRepository> { TestStockEventRepository() }

                // Provide a deterministic location API host for tests
                single { LocationApiClient("http://location.api.darren.org", get(), get<ApiResilienceManager>()) }

                // Stock system used by domain services
                single<StockSystem> {
                    StockSystem()
                }

                // DateTimeProvider for domain services
                single<DateTimeProvider> {
                    object : DateTimeProvider {
                        override fun now() = LocalDateTime.now()
                    }
                }

                // Re-register domain adapters/services so endpoints can resolve them in tests
                single<StockReader> {
                    StockSystemReader(get())
                }
                single<LocationValidator> {
                    LocationApiClientValidator(get())
                }
                single<StockService> {
                    StockService(get(), get())
                }

                // Snapshot repository for state persistence
                single<SnapshotRepository> { TestSnapshotRepository() }
                single<SnapshotStrategyFactory> { EventCountSnapshotStrategyFactory(get(), 1) }
                factory { DeliveryRequestHelper() }
                factory { SaleRequestHelper() }
            },
        )
}
