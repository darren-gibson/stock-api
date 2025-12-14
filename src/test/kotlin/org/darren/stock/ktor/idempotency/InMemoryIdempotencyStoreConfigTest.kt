package org.darren.stock.ktor.idempotency

import org.darren.stock.config.KoinModules
import org.darren.stock.ktor.idempotency.IdempotencyStore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class InMemoryIdempotencyStoreConfigTest {
    @AfterEach
    fun cleanup() {
        // Ensure we stop Koin between tests to avoid property leakage
        try {
            stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun `direct construction respects ttl and evicts after expiry`() {
        val store = InMemoryIdempotencyStore(ttlSeconds = 1, maximumSize = 10)

        store.put("req-1", 200, "body1", "application/json", "hash1")
        assertNotNull(store.get("req-1"), "value should exist immediately after put")

        Thread.sleep(1_500)
        // force cleanup to make sure expiry is processed
        store.cleanup()

        assertNull(store.get("req-1"), "value should be evicted after TTL expiry")
    }

    @Test
    fun `direct construction respects maximum size and evicts older entries`() {
        val store = InMemoryIdempotencyStore(ttlSeconds = 3600, maximumSize = 2)

        store.put("a", 200, "A", "application/json", "hA")
        store.put("b", 200, "B", "application/json", "hB")
        store.put("c", 200, "C", "application/json", "hC")

        // At most 2 entries should remain (force cleanup to ensure eviction processed)
        store.cleanup()
        val present = listOf("a", "b", "c").mapNotNull { id -> store.get(id)?.let { id } }
        assertTrue(present.size <= 2, "no more than maximumSize entries should remain")
    }

    @Test
    fun `koin wiring uses properties for ttl and max size`() {
        val koinApp =
            startKoin {
                properties(
                    mapOf(
                        "IDEMPOTENCY_TTL_SECONDS" to "1",
                        "IDEMPOTENCY_MAX_SIZE" to "2",
                    ),
                )
                modules(KoinModules.idempotencyModule)
            }
        val store = koinApp.koin.get<IdempotencyStore>()
        assertTrue(store is InMemoryIdempotencyStore)

        // Exercise behavior consistent with the configured values
        store.put("x", 200, "x", "application/json", "hx")
        assertNotNull(store.get("x"))

        Thread.sleep(1_500)
        // force cleanup
        (store as InMemoryIdempotencyStore).cleanup()
        assertNull(store.get("x"), "Store obtained from Koin should respect TTL property")

        // Verify maximum size from Koin
        val store2 = koinApp.koin.get<IdempotencyStore>() as InMemoryIdempotencyStore
        store2.put("a", 200, "A", "application/json", "hA")
        store2.put("b", 200, "B", "application/json", "hB")
        store2.put("c", 200, "C", "application/json", "hC")

        store2.cleanup()
        val present = listOf("a", "b", "c").mapNotNull { id -> store2.get(id)?.let { id } }
        assertTrue(present.size <= 2, "Koin-provided store should respect maximum size property")
    }
}
