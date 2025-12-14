package org.darren.stock.ktor.idempotency

object IdempotencyMetrics {
    const val METER_NAME = "org.darren.stock.idempotency"
    const val HITS = "idempotency.cache.hits"
    const val MISSES = "idempotency.cache.misses"
}
