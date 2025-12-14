package org.darren.stock.ktor.idempotency

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.Meter

/**
 * ResponseCacher that records hits/misses as OpenTelemetry metrics in addition to delegating
 * to an underlying ResponseCacher implementation.
 */
class OtelResponseCacher(
    private val delegate: ResponseCacher,
    meter: Meter = GlobalOpenTelemetry.get().getMeter(IdempotencyMetrics.METER_NAME),
) : ResponseCacher {
    private val hitCounter: LongCounter = meter.counterBuilder(IdempotencyMetrics.HITS).build()
    private val missCounter: LongCounter = meter.counterBuilder(IdempotencyMetrics.MISSES).build()

    override fun get(requestId: String): IdempotentResponse? {
        val value = delegate.get(requestId)
        if (value != null) {
            hitCounter.add(1)
        } else {
            missCounter.add(1)
        }
        return value
    }

    override fun store(
        requestId: String,
        statusCode: Int,
        body: String,
        contentType: String,
        bodyHash: String,
    ) {
        delegate.store(requestId, statusCode, body, contentType, bodyHash)
    }

    override fun hitCount(): Long = delegate.hitCount()

    override fun missCount(): Long = delegate.missCount()
}
