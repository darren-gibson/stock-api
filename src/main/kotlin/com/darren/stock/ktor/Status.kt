package com.darren.stock.ktor

import kotlinx.serialization.Serializable

@Serializable
internal data class Status(val status: StatusCode) {
    enum class StatusCode {
        Healthy
    }
    companion object {
        fun healthy() = Status(StatusCode.Healthy)
    }
}
