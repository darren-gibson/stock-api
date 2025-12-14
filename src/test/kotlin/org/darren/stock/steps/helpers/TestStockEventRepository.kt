package org.darren.stock.steps.helpers

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
) : StockEventRepository {
    private var shouldFail = false
    private var failureCount = 0
    private var maxFailures = 0

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
     * Reset the repository to normal operation (no failures).
     */
    fun resetFailureSimulation() {
        shouldFail = false
        failureCount = 0
        maxFailures = 0
    }

    override fun getEvents(
        location: String,
        product: String,
    ): Iterable<StockPotEvent> = delegate.getEvents(location, product)

    override fun insert(
        location: String,
        product: String,
        event: StockPotEvent,
    ) {
        if (shouldFail && failureCount < maxFailures) {
            failureCount++
            error("Simulated repository failure for testing")
        }
        delegate.insert(location, product, event)
    }
}
