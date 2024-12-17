package org.darren.stock.ktor

import kotlinx.serialization.Serializable

@Serializable
data class ErrorDTO(val status: String)
