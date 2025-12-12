package org.darren.stock.ktor

import kotlinx.serialization.Serializable

@Serializable
data class InvalidValuesDTO(
    val invalidValues: List<String>,
)
