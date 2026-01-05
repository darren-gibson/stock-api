package org.darren.stock.ktor

import kotlinx.serialization.Serializable

/**
 * DTO representing missing required fields in a request.
 * The list is immutable and should not be modified by callers.
 */
@Serializable
data class MissingFieldsDTO(
    val missingFields: List<String>,
)
