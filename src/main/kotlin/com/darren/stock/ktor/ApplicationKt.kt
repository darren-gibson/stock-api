package com.darren.stock.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
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