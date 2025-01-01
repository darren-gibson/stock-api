package org.darren.stock.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.java.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockEventRepository
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
    routing {
        move()
        statusEndpoint()
        stockCount()
        sale()
        delivery()
        getStock()
    }
}