package org.darren.stock.config

import io.ktor.client.HttpClient
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.idempotency.IdempotencyStore
import org.darren.stock.ktor.idempotency.InMemoryIdempotencyStore
import org.darren.stock.steps.helpers.TestStockEventRepository
import org.koin.dsl.module

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
                single<org.darren.stock.domain.StockEventRepository> { TestStockEventRepository() }

                // Provide a deterministic location API host for tests
                single<LocationApiClient> { LocationApiClient("http://location.api.darren.org") }

                // Stock system used by domain services
                single<StockSystem> { StockSystem() }

                // Provide an in-memory idempotency store for tests
                single<IdempotencyStore> { InMemoryIdempotencyStore() }

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
