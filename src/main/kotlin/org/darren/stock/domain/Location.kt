package org.darren.stock.domain

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class Location(val id: String) : KoinComponent {
    private val locations by inject<LocationApiClient>()

    suspend fun isShop() = locations.isShop(id)
}