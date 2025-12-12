package org.darren.stock.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import org.darren.stock.domain.*
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.ktor.Delivery.delivery
import org.darren.stock.ktor.GetStock.getStock
import org.darren.stock.ktor.Move.move
import org.darren.stock.ktor.Sale.sale
import org.darren.stock.ktor.Status.statusEndpoint
import org.darren.stock.ktor.StockCount.stockCount
import org.darren.stock.persistence.InMemoryStockEventRepository
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.fileProperties
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime

fun main(args: Array<String>) {
    startKoin {
        fileProperties()
        modules(
            module { single<HttpClientEngine> { CIO.create() } },
            module { single { LocationApiClient(getProperty("LOCATION_API")) } },
            module { single<StockSystem> { StockSystem() } },
            module { single<StockEventRepository> { InMemoryStockEventRepository() } },
            module {
                single<DateTimeProvider> {
                    object : DateTimeProvider {
                        override fun now() = LocalDateTime.now()
                    }
                }
            },
        )
    }
    io.ktor.server.netty.EngineMain
        .main(args)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
    install(ContentNegotiation) {
        json(
            Json {
                decodeEnumsCaseInsensitive = true
            },
        )
    }
    install(StatusPages) {
        handleExceptions()
    }
    routing {
        move()
        statusEndpoint()
        stockCount()
        sale()
        delivery()
        getStock()
    }
}

private fun StatusPagesConfig.handleExceptions() {
    exception<Throwable> { call, cause ->

        when (cause) {
            is LocationNotFoundException -> call.respond(NotFound, ErrorDTO("LocationNotFound"))
            is LocationNotTrackedException -> respondWithRedirectToTrackedLocation(call, cause.locationId)
            is InsufficientStockException -> call.respond(BadRequest, ErrorDTO("InsufficientStock"))
            is BadRequestException -> {
                val missingFields = getMissingFields(cause)
                if (missingFields != null) {
                    call.respond(BadRequest, MissingFieldsDTO(missingFields))
                } else {
                    val invalidValues = getInvalidValues(cause)
                    if (invalidValues != null) {
                        call.respond(BadRequest, InvalidValuesDTO(invalidValues))
                    } else {
                        throw cause
                    }
                }
            }
            else -> throw cause
        }
    }
}

private suspend fun respondWithRedirectToTrackedLocation(
    call: ApplicationCall,
    locationId: String,
) {
    val locations by inject<LocationApiClient>(LocationApiClient::class.java)
    try {
        val path = locations.getPath(locationId).reversed()
        val firstTrackedParent = path.first { it.isTracked }
        val newLocation = call.request.path().replace("/$locationId/", "/${firstTrackedParent.id}/")
        call.response.headers.append(HttpHeaders.Location, newLocation)
        call.respond(HttpStatusCode.SeeOther, ErrorDTO("LocationNotTracked"))
    } catch (e: NoSuchElementException) {
        call.respond(BadRequest, ErrorDTO("LocationNotTracked"))
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun getMissingFields(cause: Throwable): List<String>? {
    if (cause is MissingFieldException) return cause.missingFields
    if (cause.cause != null) {
        return getMissingFields(cause.cause!!)
    }
    return null
}

fun getInvalidValues(cause: Throwable): List<String>? {
    if (cause is InvalidValuesException) return cause.fields
    if (cause.cause != null) {
        return getInvalidValues(cause.cause!!)
    }
    return null
}
