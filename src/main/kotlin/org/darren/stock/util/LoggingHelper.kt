package org.darren.stock.util

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import java.io.IOException

object LoggingHelper {
    suspend fun wrapHttpCallWithLogging(
        logger: KLogger,
        block: suspend () -> HttpResponse,
    ): HttpResponse =
        try {
            val response = block()
            logger.debug { "call to ${response.call.request.url} = ${response.status}, ${response.headers}" }
            response
        } catch (e: ResponseException) {
            logHttpException(logger, e)
            throw e
        } catch (e: HttpRequestTimeoutException) {
            logHttpException(logger, e)
            throw e
        } catch (e: ConnectTimeoutException) {
            logHttpException(logger, e)
            throw e
        } catch (e: SocketTimeoutException) {
            logHttpException(logger, e)
            throw e
        } catch (e: IOException) {
            logHttpException(logger, e)
            throw e
        }

    private fun logHttpException(
        logger: KLogger,
        e: Exception,
    ) {
        when (e) {
            is ResponseException -> logger.warn { "HTTP call failed with response: ${e.response.status}" }
            is HttpRequestTimeoutException -> logger.warn { "HTTP request timeout: ${e.message}" }
            is ConnectTimeoutException -> logger.warn { "HTTP call timed out: ${e.message}" }
            is SocketTimeoutException -> logger.warn { "HTTP socket timed out: ${e.message}" }
            is IOException -> logger.warn { "HTTP I/O error: ${e.message}" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    inline fun <T> KLogger.logOperation(
        operation: String,
        block: () -> T,
    ): T {
        debug { "Starting $operation" }
        return try {
            block().also { debug { "Completed $operation" } }
        } catch (e: Throwable) {
            error(e) { "Failed $operation" }
            throw e
        }
    }
}
