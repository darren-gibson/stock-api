package org.darren.stock.ktor

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.darren.stock.config.KoinModules
import org.darren.stock.ktor.Delivery.deliveryEndpoint
import org.darren.stock.ktor.GetStock.getStockEndpoint
import org.darren.stock.ktor.Move.moveEndpoint
import org.darren.stock.ktor.Sale.saleEndpoint
import org.darren.stock.ktor.Status.statusEndpoint
import org.darren.stock.ktor.StockCount.stockCountEndpoint
import org.darren.stock.ktor.exception.ExceptionHandlerChain
import org.koin.core.context.startKoin
import org.koin.fileProperties

fun main(args: Array<String>) {
    startKoin {
        fileProperties()
        modules(KoinModules.allModules())
    }
    io.ktor.server.netty.EngineMain
        .main(args)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
    install(ContentNegotiation) {
        json(
            Json { decodeEnumsCaseInsensitive = true },
        )
    }
    install(StatusPages) { handleExceptions() }
    routing {
        moveEndpoint()
        statusEndpoint()
        stockCountEndpoint()
        saleEndpoint()
        deliveryEndpoint()
        getStockEndpoint()
    }
}

private fun StatusPagesConfig.handleExceptions() {
    exception<Throwable> { call, cause ->
        if (!ExceptionHandlerChain.handle(call, cause)) {
            throw cause
        }
    }
}
