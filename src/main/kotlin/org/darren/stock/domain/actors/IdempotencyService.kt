package org.darren.stock.domain.actors

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.Meter
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.actors.StockPotProtocol.StockPotRequest
import org.darren.stock.domain.stockSystem.IdempotencyContentMismatchException

object DomainIdempotencyMetrics {
    private val meter: Meter by lazy { GlobalOpenTelemetry.get().getMeter("org.darren.stock.domain.idempotency") }
    val HITS: LongCounter by lazy {
        meter
            .counterBuilder("idempotency.domain.hits")
            .setDescription("Number of times a duplicate request was detected and skipped at the domain level")
            .build()
    }
    val MISSES: LongCounter by lazy {
        meter
            .counterBuilder("idempotency.domain.misses")
            .setDescription("Number of times a new request was processed (not a duplicate)")
            .build()
    }
}

class IdempotencyService(
    private val repository: StockEventRepository,
) {
    suspend fun checkIdempotency(protocol: StockPotRequest): Boolean {
        val existingEvents = repository.getEventsByRequestId(protocol.requestId)
        return if (existingEvents.any()) {
            val event = existingEvents.first()
            val existingContentHash = event.contentHash
            if (existingContentHash == protocol.contentHash()) {
                DomainIdempotencyMetrics.HITS.add(1)
                true // Already processed
            } else {
                throw IdempotencyContentMismatchException("Content hash mismatch for request ${protocol.requestId}", protocol.requestId)
            }
        } else {
            DomainIdempotencyMetrics.MISSES.add(1)
            false // Not processed
        }
    }
}
