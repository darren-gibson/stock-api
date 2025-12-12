package org.darren.stock.domain

class LocationNotFoundException(
    val locationId: String,
) : Exception("location '$locationId' not found")
