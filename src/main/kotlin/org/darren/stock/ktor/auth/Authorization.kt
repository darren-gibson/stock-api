package org.darren.stock.ktor.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

/**
 * Error response for authorization failures.
 */
@Serializable
internal data class AuthzErrorDTO(
    val status: String,
)

/**
 * Represents a permission in the system.
 * Format: {resource}:{operation}:{action}
 * Example: "stock:count:write", "stock:movement:read"
 */
data class Permission(
    val resource: String,
    val operation: String,
    val action: String,
) {
    override fun toString(): String = "$resource:$operation:$action"

    companion object {
        fun parse(permission: String): Permission? {
            val parts = permission.split(":")
            if (parts.size != 3) return null
            return Permission(parts[0], parts[1], parts[2])
        }
    }
}

/**
 * Job function definitions with their associated permissions.
 * This represents the role-based access control model.
 */
object JobPermissions {
    private val permissionsByJob =
        mapOf(
            "Regional Stock Auditor" to
                listOf(
                    Permission("stock", "count", "read"),
                    Permission("stock", "level", "read"),
                    Permission("stock", "movement", "read"),
                ),
            "Store Stock Controller" to
                listOf(
                    Permission("stock", "count", "read"),
                    Permission("stock", "count", "write"),
                    Permission("stock", "level", "read"),
                    Permission("stock", "movement", "read"),
                    Permission("stock", "movement", "write"),
                ),
            "Warehouse Manager" to
                listOf(
                    Permission("stock", "count", "read"),
                    Permission("stock", "count", "write"),
                    Permission("stock", "level", "read"),
                    Permission("stock", "movement", "read"),
                    Permission("stock", "movement", "write"),
                ),
            "System Administrator" to
                listOf(
                    Permission("stock", "count", "read"),
                    Permission("stock", "count", "write"),
                    Permission("stock", "level", "read"),
                    Permission("stock", "movement", "read"),
                    Permission("stock", "movement", "write"),
                    Permission("job", "permission", "read"),
                    Permission("job", "permission", "write"),
                ),
        )

    fun getPermissions(job: String): List<Permission> = permissionsByJob[job] ?: emptyList()

    fun hasPermission(
        job: String,
        permission: Permission,
    ): Boolean = getPermissions(job).contains(permission)
}

/**
 * Checks if the authenticated principal has the required permission.
 * For location-scoped jobs, also validates that the request location is within scope.
 *
 * @param permission The required permission
 * @param requestLocation The location being accessed (optional)
 * @return true if authorized, false otherwise
 */
suspend fun ApplicationCall.authorize(
    permission: Permission,
    requestLocation: String? = null,
): Boolean {
    val principal = attributes.getOrNull(ColleaguePrincipalKey)

    if (principal == null) {
        respond(HttpStatusCode.Unauthorized, AuthzErrorDTO("Unauthorized"))
        return false
    }

    // Check if job has the required permission
    if (!JobPermissions.hasPermission(principal.job, permission)) {
        logger.warn { "Permission denied for ${principal.job}: missing $permission" }
        respond(HttpStatusCode.Forbidden, AuthzErrorDTO("PermissionDenied"))
        return false
    }

    // Check location scope if applicable
    if (requestLocation != null && principal.locations.isNotEmpty()) {
        if (!principal.locations.contains(requestLocation)) {
            logger.warn {
                "Location scope violation: ${principal.job} attempted to access $requestLocation, " +
                    "but is scoped to ${principal.locations}"
            }
            respond(HttpStatusCode.Forbidden, AuthzErrorDTO("PermissionDenied"))
            return false
        }
    }

    return true
}

/**
 * Extension to check authorization without responding.
 * Useful for conditional logic without side effects.
 */
fun ApplicationCall.isAuthorized(
    permission: Permission,
    requestLocation: String? = null,
): Boolean {
    val principal = attributes.getOrNull(ColleaguePrincipalKey) ?: return false

    if (!JobPermissions.hasPermission(principal.job, permission)) {
        return false
    }

    if (requestLocation != null && principal.locations.isNotEmpty()) {
        if (!principal.locations.contains(requestLocation)) {
            return false
        }
    }

    return true
}
