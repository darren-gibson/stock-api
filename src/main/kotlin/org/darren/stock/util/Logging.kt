package org.darren.stock.util

import io.github.oshai.kotlinlogging.KLogger
import io.opentelemetry.api.trace.Span
import org.slf4j.MDC

const val TRACE_ID = "trace_id"

/**
 * Put the given trace id into the SLF4J MDC for the current thread.
 */
fun putTraceId(id: String) = MDC.put(TRACE_ID, id)

/**
 * Read trace id from MDC or return null.
 */
fun currentTraceId(): String? = MDC.get(TRACE_ID)

/**
 * Get the current trace ID from OpenTelemetry context.
 */
fun currentOpenTelemetryTraceId(): String? {
    val currentSpan = Span.current()
    return if (currentSpan.spanContext.isValid) {
        currentSpan.spanContext.traceId
    } else {
        null
    }
}

/**
 * Wraps an HTTP call with logging, ensuring trace context is propagated.
 */
suspend fun <T> wrapHttpCallWithLogging(
    logger: KLogger,
    block: suspend () -> T,
): T {
    logger.debug { "Making HTTP call" }
    return try {
        val result = block()
        logger.debug { "HTTP call completed successfully" }
        result
    } catch (e: Exception) {
        logger.error(e) { "HTTP call failed" }
        throw e
    }
}
