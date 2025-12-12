package org.darren.stock.ktor

import kotlinx.serialization.Serializable

@Serializable
data class MissingFieldsDTO(
    val missingFields: List<String>,
)
