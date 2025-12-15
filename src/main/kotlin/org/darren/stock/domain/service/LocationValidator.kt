package org.darren.stock.domain.service

import org.darren.stock.domain.LocationApiClient

/** Adapter to validate locations - allows tests to provide a lightweight implementation. */
interface LocationValidator {
    suspend fun ensureValidLocation(locationId: String)
}

class LocationApiClientValidator(
    private val client: LocationApiClient,
) : LocationValidator {
    override suspend fun ensureValidLocation(locationId: String) {
        client.ensureValidLocation(locationId)
    }
}
