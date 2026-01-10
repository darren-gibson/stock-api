package org.darren.stock.domain

class LocationApiUnavailableException(
    val status: String,
) : Exception("Location API unavailable: $status")
