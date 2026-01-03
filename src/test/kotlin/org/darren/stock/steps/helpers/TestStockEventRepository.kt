package org.darren.stock.steps.helpers

import kotlinx.coroutines.delay
import org.darren.stock.domain.IdempotencyStatus
import org.darren.stock.domain.RepositoryFailureException
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.actors.events.StockPotEvent
import org.darren.stock.persistence.InMemoryStockEventRepository

/**
 * Test version of StockEventRepository that can simulate failures using the decorator pattern.
 * Wraps an InMemoryStockEventRepository and delegates all calls except when simulating failures.
 * Allows tests to validate error handling and idempotency behavior for 5xx errors.
 */
class TestStockEventRepository(
    private val delegate: StockEventRepository = InMemoryStockEventRepository(),
    delayMillis: Long = 0,
) : StockEventRepository {
    private var currentDelayMillis = delayMillis
    private var shouldFail = false
    private var failureCount = 0
    private var maxFailures = 0
    private val failingProducts = mutableSetOf<String>()
    private val productFailureCounts = mutableMapOf<String, Int>()
    private val productMaxFailures = mutableMapOf<String, Int>()

    /**
     * Configure the repository to fail for the next N insert operations.
     * @param count Number of insert operations that should fail
     */
    fun failNextInserts(count: Int) {
        shouldFail = true
        failureCount = 0
        maxFailures = count
    }

    /**
     * Configure the repository to fail for inserts of specific products up to N times.
     * @param products Set of product IDs that should fail on insert
     * @param maxFailuresPerProduct Maximum number of failures per product
     */
    fun failInsertsForProducts(
        products: Set<String>,
        maxFailuresPerProduct: Int = Int.MAX_VALUE,
    ) {
        failingProducts.clear()
        failingProducts.addAll(products)
        productMaxFailures.clear()
        productFailureCounts.clear()
        products.forEach { product ->
            productMaxFailures[product] = maxFailuresPerProduct
            productFailureCounts[product] = 0
        }
    }

    /**
     * Reset the repository to normal operation (no failures).
     */
    fun resetFailureSimulation() {
        shouldFail = false
        failureCount = 0
        maxFailures = 0
        failingProducts.clear()
        productFailureCounts.clear()
        productMaxFailures.clear()
    }

    override suspend fun getEventsInChronologicalOrder(
        location: String,
        product: String,
    ): Iterable<StockPotEvent> = delegate.getEventsInChronologicalOrder(location, product)

    override suspend fun insert(
        location: String,
        product: String,
        event: StockPotEvent,
    ) {
        if (currentDelayMillis > 0) {
            delay(currentDelayMillis)
        }
        if (failingProducts.contains(product) || (shouldFail && failureCount < maxFailures)) {
            if (failingProducts.contains(product)) {
                val currentFailures = productFailureCounts.getOrDefault(product, 0)
                val maxFailuresForProduct = productMaxFailures.getOrDefault(product, Int.MAX_VALUE)
                if (currentFailures < maxFailuresForProduct) {
                    productFailureCounts[product] = currentFailures + 1
                    throw RepositoryFailureException("Simulated repository failure for product $product (attempt ${currentFailures + 1})")
                }
            } else {
                failureCount++
                throw RepositoryFailureException("Simulated repository failure for testing")
            }
        }
        delegate.insert(location, product, event)
    }

    override suspend fun getEventsByRequestId(requestId: String): Iterable<StockPotEvent> = delegate.getEventsByRequestId(requestId)

    override suspend fun getEventsAfterRequestIdInChronologicalOrder(
        location: String,
        product: String,
        afterRequestId: String,
    ): Iterable<StockPotEvent> = delegate.getEventsAfterRequestIdInChronologicalOrder(location, product, afterRequestId)

    override suspend fun getLastPersistedRequestId(
        location: String,
        product: String,
    ): String? = delegate.getLastPersistedRequestId(location, product)

    override suspend fun checkIdempotencyStatus(
        requestId: String,
        contentHash: String,
    ): IdempotencyStatus = delegate.checkIdempotencyStatus(requestId, contentHash)

    /**
     * Directly insert an event into the repository, bypassing failure simulation.
     * Used for testing out-of-order event scenarios.
     */
    suspend fun insertEventDirectly(
        location: String,
        product: String,
        event: StockPotEvent,
    ) {
        delegate.insert(location, product, event)
    }
}
