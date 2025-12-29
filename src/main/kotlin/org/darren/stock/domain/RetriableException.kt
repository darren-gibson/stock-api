package org.darren.stock.domain

open class RetriableException(
    message: String?,
    cause: Throwable?,
) : Exception(message, cause)
