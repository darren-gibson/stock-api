package org.darren.stock.ktor.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.koin.java.KoinJavaComponent.inject

private val logger = KotlinLogging.logger {}

/**
 * Configures authentication requirements for a route and installs an interceptor.
 * The interceptor validates JWT tokens and checks permissions before route handlers execute.
 *
 * Example:
 * ```kotlin
 * route("/locations/{locationId}/products/{productId}/sales") {
 *     requiresAuth(Permission("stock", "movement", "write"), "locationId")
 *
 *     post {
 *         // Handler code - authentication/authorization already validated
 *         val principal = call.principal!! // Guaranteed non-null
 *     }
 * }
 * ```
 */
fun Route.requiresAuth(
    permission: Permission,
    locationParam: String? = null,
) {
    install(
        createRouteScopedPlugin("AuthenticationPlugin") {
            on(CallSetup) { call ->
                val jwtConfig by inject<JwtConfig>(JwtConfig::class.java)

                // Authenticate
                if (call.authenticate(jwtConfig) == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@on
                }

                // Authorize
                val locationId = locationParam?.let { call.parameters[it] }
                if (!call.authorize(permission, locationId)) {
                    return@on
                }
            }
        },
    )
}
