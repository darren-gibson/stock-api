package com.darren.stock.ktor

import com.darren.stock.domain.actors.LocationMessages
import com.darren.stock.domain.StockEventRepository
import com.darren.stock.domain.StockSystem
import com.darren.stock.domain.actors.LocationActor.Companion.locationActor
import com.darren.stock.persistence.InMemoryStockEventRepository
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import org.koin.core.context.startKoin
import org.koin.dsl.module

@OptIn(DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    startKoin {
        modules(
            module { single<SendChannel<LocationMessages>> { GlobalScope.locationActor() } },
            module { single<StockSystem> { StockSystem() } },
            module { single<StockEventRepository> { InMemoryStockEventRepository() } }
        )
    }
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
//    val repo by inject<StockEventRepository>(StockEventRepository::class.java)
    install(ContentNegotiation) {
        json()
    }
    routing {
        statusEndpoint()
        stockCount()
        sale()
    }
}

fun Routing.statusEndpoint() {
    get("/_status") {
        call.respond(HttpStatusCode.OK, Status.healthy())
    }
}