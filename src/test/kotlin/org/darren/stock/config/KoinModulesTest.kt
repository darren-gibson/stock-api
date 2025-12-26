package org.darren.stock.config

import io.github.smyrgeorge.actor4k.system.ActorSystem
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.time.Duration.Companion.seconds

class KoinModulesTest {
    @AfterEach
    fun tearDown() {
        try {
            stopKoin()
        } catch (_: Exception) {
        }
        // Also shutdown ActorSystem
        runBlocking {
            try {
                withTimeout(2.seconds) { ActorSystem.shutdown() }
            } catch (_: Exception) {
            }
        }
    }

    @Test
    fun `koin modules load and provide core bindings`() {
        val koinApp =
            startKoin {
                // Provide minimal properties needed by modules that read properties
                properties(
                    mapOf(
                        "LOCATION_API" to "http://localhost",
                        "IDEMPOTENCY_TTL_SECONDS" to "60",
                        "IDEMPOTENCY_MAX_SIZE" to "100",
                    ),
                )
                modules(KoinModules.allModules())
            }

        // Resolve a couple of core bindings to ensure the modules wire correctly
        val stockService = koinApp.koin.get<org.darren.stock.domain.service.StockService>()
        val repository = koinApp.koin.get<org.darren.stock.domain.StockEventRepository>()
        val dateTimeProvider = koinApp.koin.get<org.darren.stock.domain.DateTimeProvider>()

        assertNotNull(stockService)
        assertNotNull(repository)
        assertNotNull(dateTimeProvider)
    }
}
