package org.darren.stock.ktor

import io.ktor.client.engine.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.stockSystem.StockSystem
import org.darren.stock.persistence.InMemoryStockEventRepository
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.fileProperties
import io.ktor.client.engine.java.*

fun main(args: Array<String>) {
    startKoin {
        fileProperties()
        modules(
            module { single<HttpClientEngine> { Java.create() } },
            module { single { params -> LocationApiClient(getProperty("LOCATION_API")) } },
            module { single<StockSystem> { StockSystem() } },
            module { single<StockEventRepository> { InMemoryStockEventRepository() } }
        )
    }
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    routing {
        statusEndpoint()
        stockCount()
        sale()
        delivery()
    }
}

fun Routing.statusEndpoint() {
    get("/_status") {
        call.respond(HttpStatusCode.OK, Status.healthy())
    }
}