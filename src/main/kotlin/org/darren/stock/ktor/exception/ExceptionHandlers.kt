package org.darren.stock.ktor.exception

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.SerializationException
import org.darren.stock.domain.InsufficientStockException
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.LocationNotFoundException
import org.darren.stock.domain.LocationNotTrackedException
import org.darren.stock.ktor.ErrorDTO
import org.darren.stock.ktor.InvalidValuesDTO
import org.darren.stock.ktor.MissingFieldsDTO
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
 * Handles LocationNotFoundException by responding with 404.
 */
object LocationNotFoundHandler : ExceptionHandler {
    override suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ): Boolean {
        if (cause is LocationNotFoundException) {
            call.respond(NotFound, ErrorDTO("LocationNotFound"))
            return true
        }
        return false
    }
}

/**
 * Handles LocationNotTrackedException by redirecting to the first tracked parent location.
 */
object LocationNotTrackedHandler : ExceptionHandler {
    override suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ): Boolean {
        if (cause is LocationNotTrackedException) {
            val locations by inject<LocationApiClient>(LocationApiClient::class.java)
            try {
                val path = locations.getPath(cause.locationId).reversed()
                val firstTrackedParent = path.first { it.isTracked }
                val newLocation = call.request.path().replace("/${cause.locationId}/", "/${firstTrackedParent.id}/")
                call.response.headers.append(HttpHeaders.Location, newLocation)
                call.respond(HttpStatusCode.SeeOther, ErrorDTO("LocationNotTracked"))
            } catch (e: NoSuchElementException) {
                call.respond(BadRequest, ErrorDTO("LocationNotTracked"))
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
            call.respond(BadRequest, ErrorDTO("InsufficientStock"))
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

            call.respond(BadRequest, ErrorDTO("BadRequest"))
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
            LocationNotFoundHandler,
            LocationNotTrackedHandler,
            InsufficientStockHandler,
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
