package org.darren.stock.util

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.statement.*

object LoggingHelper {
    suspend fun wrapHttpCallWithLogging(
        logger: KLogger,
        block: suspend () -> HttpResponse,
    ): HttpResponse {
        try {
            val response = block()

            logger.debug { "call to ${response.call.request.url} = ${response.status}, ${response.headers}" }
            return response
        } catch (e: Exception) {
            logger.warn { "call failed with exception: ${e.message}" }
            throw e
        }
    }
}
