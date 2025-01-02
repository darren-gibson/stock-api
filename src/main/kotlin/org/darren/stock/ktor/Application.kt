package org.darren.stock.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.java.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
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
import java.time.LocalDateTime

fun main(args: Array<String>) {
    startKoin {
        fileProperties()
        modules(
            module { single<HttpClientEngine> { Java.create() } },
            module { single { LocationApiClient(getProperty("LOCATION_API")) } },
            module { single<StockSystem> { StockSystem() } },
            module { single<StockEventRepository> { InMemoryStockEventRepository() } },
            module {
                single<DateTimeProvider> {
                    object : DateTimeProvider {
                        override fun now() = LocalDateTime.now()
                    }
                }
            }
        )
    }
    io.ktor.server.netty.EngineMain.main(args)
}


@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            decodeEnumsCaseInsensitive = true
        })
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
        if (cause is LocationNotFoundException)
            call.respond(NotFound, ErrorDTO("LocationNotFound"))
        else if (cause is OperationNotSupportedException)
            call.respond(Conflict, ErrorDTO("LocationNotSupported"))
        else if (cause is BadRequestException) {
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
        } else {
            throw cause
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun getMissingFields(cause: Throwable): List<String>? {
    if (cause is MissingFieldException) return cause.missingFields
    if (cause.cause != null)
        return getMissingFields(cause.cause!!)
    return null
}

fun getInvalidValues(cause: Throwable): List<String>? {
    if (cause is InvalidValuesException) return cause.fields
    if (cause.cause != null)
        return getInvalidValues(cause.cause!!)
    return null
}
