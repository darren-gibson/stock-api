package org.darren.stock.ktor.exception

/**
 * Standard error codes returned by the API.
 * These constants ensure consistent error messaging across endpoints.
 */
object ErrorCodes {
    const val LOCATION_NOT_FOUND = "LocationNotFound"
    const val LOCATION_NOT_TRACKED = "LocationNotTracked"
    const val INSUFFICIENT_STOCK = "InsufficientStock"
    const val IDEMPOTENCY_CONTENT_MISMATCH = "IdempotencyContentMismatch"
    const val BAD_REQUEST = "BadRequest"
    const val UNAUTHORIZED = "Unauthorized"
    const val PERMISSION_DENIED = "PermissionDenied"
    const val UPSTREAM_SERVICE_UNAVAILABLE = "UpstreamServiceUnavailable"
}
