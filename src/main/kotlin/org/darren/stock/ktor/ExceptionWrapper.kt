package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import org.darren.stock.domain.LocationNotFoundException
import org.darren.stock.domain.OperationNotSupportedException
import java.time.format.DateTimeParseException

object ExceptionWrapper {
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun runWithExceptionHandling(call: ApplicationCall, dateField: String = "unknown", block: suspend () -> Unit) {
        try {
            block()
        } catch (e: LocationNotFoundException) {
            call.respond(NotFound, ErrorDTO("LocationNotFound"))
        } catch (e: OperationNotSupportedException) {
            call.respond(Conflict, ErrorDTO("LocationNotSupported"))
        } catch (e: BadRequestException) {
            if (e.cause?.cause is MissingFieldException) {
                val missing: MissingFieldException = e.cause?.cause as MissingFieldException
                call.respond(BadRequest, MissingFieldsDTO(missing.missingFields))
            } else if(e.cause?.cause is DateTimeParseException) {
                call.respond(BadRequest, InvalidValuesDTO(listOf(dateField)))
            } else {
                throw e
            }
        }
    }
}