package org.darren.stock.ktor

import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.darren.stock.domain.service.StockService
import org.darren.stock.ktor.auth.Permission
import org.darren.stock.ktor.auth.PermissionConstants.Actions.READ
import org.darren.stock.ktor.auth.PermissionConstants.Operations.LEVEL
import org.darren.stock.ktor.auth.PermissionConstants.Resources.STOCK
import org.darren.stock.ktor.auth.requiresAuth
import org.koin.java.KoinJavaComponent.inject

object GetStock {
    fun Routing.getStockEndpoint() {
        route("/locations/{locationId}/products/{productId}") {
            requiresAuth(Permission(STOCK, LEVEL, READ), "locationId")

            get {
                val locationId = call.parameters["locationId"]!!
                val productId = call.parameters["productId"]!!
                val includeChildren = call.parameters["includeChildren"]?.toBoolean() ?: true
                val stockService by inject<StockService>(StockService::class.java)

                val response = stockService.getStockResponse(locationId, productId, includeChildren)
                call.respond(OK, response)
            }
        }
    }

    // Response DTOs moved to `StockService` to keep endpoint thin. The endpoint simply
    // delegates to the service and returns the service's DTO.
}
