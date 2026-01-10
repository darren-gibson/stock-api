package org.darren.stock.ktor.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadGateway
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.SerializationException
import org.darren.stock.domain.InsufficientStockException
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.LocationApiUnavailableException
import org.darren.stock.domain.LocationNotFoundException
import org.darren.stock.domain.LocationNotTrackedException
import org.darren.stock.domain.stockSystem.IdempotencyContentMismatchException
import org.darren.stock.ktor.ErrorDTO
import org.darren.stock.ktor.InvalidValuesDTO
import org.darren.stock.ktor.MissingFieldsDTO
import org.darren.stock.ktor.exception.ErrorCodes.BAD_REQUEST
import org.darren.stock.ktor.exception.ErrorCodes.IDEMPOTENCY_CONTENT_MISMATCH
import org.darren.stock.ktor.exception.ErrorCodes.INSUFFICIENT_STOCK
import org.darren.stock.ktor.exception.ErrorCodes.LOCATION_NOT_FOUND
import org.darren.stock.ktor.exception.ErrorCodes.LOCATION_NOT_TRACKED
import org.darren.stock.ktor.exception.ErrorCodes.UPSTREAM_SERVICE_UNAVAILABLE
import org.koin.java.KoinJavaComponent.inject

/**
 * Strategy interface for handling specific exception types.
 */
interface ExceptionHandler {
    suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ): Boolean
}

/**
 * Handles upstream availability issues (fail-fast or downstream 5xx) by returning 502.
 */
object UpstreamServiceHandler : ExceptionHandler {
    override suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ): Boolean {
        val upstream = findUpstreamCause(cause)
        if (upstream != null) {
            call.respond(BadGateway, ErrorDTO(UPSTREAM_SERVICE_UNAVAILABLE))
            return true
        }
        return false
    }

    private tailrec fun findUpstreamCause(throwable: Throwable?): Throwable? {
        if (throwable == null) return null
        if (throwable is LocationApiUnavailableException) {
            return throwable
        }
        return findUpstreamCause(throwable.cause)
    }
}

/**
 * Handles LocationNotFoundException by responding with 404.
 */
object LocationNotFoundHandler : ExceptionHandler {
    override suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ): Boolean {
        if (cause is LocationNotFoundException) {
            call.respond(NotFound, ErrorDTO(LOCATION_NOT_FOUND))
            return true
        }
        return false
    }
}

/**
 * Handles LocationNotTrackedException by redirecting to the first tracked parent location.
 */
object LocationNotTrackedHandler : ExceptionHandler {
    private val logger = KotlinLogging.logger {}

    override suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ): Boolean {
        if (cause is LocationNotTrackedException) {
            val locations by inject<LocationApiClient>(LocationApiClient::class.java)
            val path = locations.getPath(cause.locationId).reversed()
            val firstTrackedParent = path.firstOrNull { it.isTracked }
            if (firstTrackedParent != null) {
                val newLocation = call.request.path().replace("/${cause.locationId}/", "/${firstTrackedParent.id}/")
                call.response.headers.append(HttpHeaders.Location, newLocation)
                call.respond(HttpStatusCode.SeeOther, ErrorDTO(LOCATION_NOT_TRACKED))
            } else {
                logger.warn { "Location ${cause.locationId} has no tracked parent, returning 400" }
                call.respond(BadRequest, ErrorDTO(LOCATION_NOT_TRACKED))
            }
            return true
        }
        return false
    }
}

/**
 * Handles InsufficientStockException by responding with 400.
 */
object InsufficientStockHandler : ExceptionHandler {
    override suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ): Boolean {
        if (cause is InsufficientStockException) {
            call.respond(BadRequest, ErrorDTO(INSUFFICIENT_STOCK))
            return true
        }
        return false
    }
}

/**
 * Handles IdempotencyContentMismatchException by responding with 409 Conflict.
 */
object IdempotencyContentMismatchHandler : ExceptionHandler {
    override suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ): Boolean {
        if (cause is IdempotencyContentMismatchException) {
            call.respond(HttpStatusCode.Conflict, ErrorDTO(IDEMPOTENCY_CONTENT_MISMATCH))
            return true
        }
        return false
    }
}

/**
 * Handles BadRequestException including MissingFieldException and InvalidValuesException.
 */
object BadRequestHandler : ExceptionHandler {
    override suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ): Boolean {
        if (cause is BadRequestException || cause is SerializationException) {
            val missingFields = RecursiveFieldExtractor.extractMissingFields(cause)
            if (missingFields != null) {
                call.respond(BadRequest, MissingFieldsDTO(missingFields))
                return true
            }

            val invalidValues = RecursiveFieldExtractor.extractInvalidValues(cause)
            if (invalidValues != null) {
                call.respond(BadRequest, InvalidValuesDTO(invalidValues))
                return true
            }

            call.respond(BadRequest, ErrorDTO(BAD_REQUEST))
            return true
        }
        return false
    }
}

/**
 * Chain of Responsibility coordinator for exception handling.
 */
object ExceptionHandlerChain {
    private val handlers =
        listOf(
            UpstreamServiceHandler,
            LocationNotFoundHandler,
            LocationNotTrackedHandler,
            InsufficientStockHandler,
            IdempotencyContentMismatchHandler,
            BadRequestHandler,
        )

    suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ): Boolean {
        for (handler in handlers) {
            if (handler.handle(call, cause)) {
                return true
            }
        }
        return false
    }
}
