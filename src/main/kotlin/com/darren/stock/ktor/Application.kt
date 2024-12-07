package com.darren.stock.ktor

import com.darren.stock.domain.StockEventRepository
import com.darren.stock.persistence.InMemoryStockEventRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject

fun main(args: Array<String>): Unit {
    startKoin {
        module {
            single<StockEventRepository> { InMemoryStockEventRepository() }
        }
    }
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
//    val repo by inject<StockEventRepository>(StockEventRepository::class.java)
    install(ContentNegotiation)
    routing {
        statusEndpoint()
    }
}

fun Routing.statusEndpoint() {
    get("/_status") {
        call.respond(HttpStatusCode.OK)
    }
}