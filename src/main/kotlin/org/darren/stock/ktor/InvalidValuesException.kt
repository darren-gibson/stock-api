package org.darren.stock.ktor

import kotlinx.serialization.SerializationException

class InvalidValuesException(
    val fields: List<String>,
    cause: Throwable?,
) : SerializationException(cause)
