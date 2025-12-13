package org.darren.stock.ktor.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import java.security.PublicKey

private val logger = KotlinLogging.logger {}

/**
 * Authentication principal representing an authenticated colleague.
 * Contains user identity and authorization context extracted from JWT token.
 */
data class ColleaguePrincipal(
    val sub: String,
    val name: String,
    val job: String,
    val locations: List<String>,
)

/**
 * Key for storing authenticated principal in call attributes.
 */
val ColleaguePrincipalKey = AttributeKey<ColleaguePrincipal>("ColleaguePrincipal")

/**
 * Configuration for JWT authentication.
 */
data class JwtConfig(
    val publicKey: PublicKey,
    val issuer: String,
    val audience: String,
)

/**
 * Error response for authentication failures.
 */
@Serializable
internal data class AuthErrorDTO(
    val status: String,
)

/**
 * Validates JWT token and extracts authentication principal.
 *
 * @return ColleaguePrincipal if token is valid, null otherwise
 */
fun validateToken(
    token: String,
    config: JwtConfig,
): ColleaguePrincipal? =
    try {
        val claims =
            Jwts
                .parser()
                .verifyWith(config.publicKey as java.security.interfaces.RSAPublicKey)
                .requireIssuer(config.issuer)
                .requireAudience(config.audience)
                .build()
                .parseSignedClaims(token)
                .payload

        val sub = claims.subject ?: return null
        val name = claims["name"] as? String ?: return null
        val job = claims["job"] as? String ?: return null
        val locations = extractLocations(claims)

        ColleaguePrincipal(
            sub = sub,
            name = name,
            job = job,
            locations = locations,
        )
    } catch (e: JwtException) {
        logger.warn(e) { "JWT validation failed: ${e.message}" }
        null
    } catch (e: IllegalArgumentException) {
        logger.warn(e) { "JWT validation failed: ${e.message}" }
        null
    }

/**
 * Extracts locations from JWT claims.
 * Handles both single string and array formats.
 */
private fun extractLocations(claims: Claims): List<String> {
    val locationClaim = claims["location"] ?: return emptyList()

    return when (locationClaim) {
        is List<*> -> locationClaim.filterIsInstance<String>()
        is String -> listOf(locationClaim)
        else -> emptyList()
    }
}

/**
 * Authentication interceptor that validates JWT tokens on all requests.
 */
suspend fun ApplicationCall.authenticate(config: JwtConfig): ColleaguePrincipal? {
    val authHeader = request.headers[HttpHeaders.Authorization]

    if (authHeader == null) {
        respond(HttpStatusCode.Unauthorized, AuthErrorDTO("Unauthorized"))
        return null
    }

    if (!authHeader.startsWith("Bearer ")) {
        respond(HttpStatusCode.Unauthorized, AuthErrorDTO("Unauthorized"))
        return null
    }

    val token = authHeader.removePrefix("Bearer ")
    val principal = validateToken(token, config)

    if (principal == null) {
        respond(HttpStatusCode.Unauthorized, AuthErrorDTO("Unauthorized"))
        return null
    }

    // Store principal in call attributes for use in authorization checks
    attributes.put(ColleaguePrincipalKey, principal)
    return principal
}

/**
 * Extension to retrieve authenticated principal from call.
 */
val ApplicationCall.principal: ColleaguePrincipal?
    get() = attributes.getOrNull(ColleaguePrincipalKey)
