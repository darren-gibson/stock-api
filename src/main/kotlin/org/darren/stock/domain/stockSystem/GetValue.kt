package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.actors.events.StockPotMessages
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.actors.events.GetValue
import org.darren.stock.domain.actors.Reply
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object GetValue : KoinComponent {
    suspend fun StockSystem.getValue(locationId: String, productId: String): Double {
        val stockPots = getAllStockPotAndAllChildrenForLocation(this, locationId, productId)

        return stockPots.map { sp ->
            val completable = CompletableDeferred<Reply>()
            sp.send(GetValue(completable))
            completable
        }.awaitAll().sumOf { it.getOrThrow() }
    }

    private suspend fun getAllStockPotAndAllChildrenForLocation(
        system: StockSystem, locationId: String, productId: String
    ): Set<SendChannel<StockPotMessages>> {
        val locationApi by inject<LocationApiClient>()
        val allLocations = locationApi.getLocationsHierarchy(locationId)

        return allStockPotsForLocations(system, locationId, allLocations, productId)
    }

    private fun allStockPotsForLocations(
        system: StockSystem, locationId: String, allLocations: Map<String, String>, productId: String
    ): Set<SendChannel<StockPotMessages>> {
        val uniqueLocations = allLocations.keys.union(allLocations.values).union(setOf(locationId)).toSet()
        return uniqueLocations.mapNotNull { system.stockPots[it to productId] }.toSet()
    }
}


