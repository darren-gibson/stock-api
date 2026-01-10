package org.darren.stock.domain

/**
 * Interface for components that support health checking.
 * Implementations should quickly check their operational status without
 * performing heavy operations.
 */
interface HealthProbe {
    /** Check if the component is healthy. Should return quickly. */
    suspend fun isHealthy(): Boolean
}
