package com.darren.stock.domain.stockSystem

import com.darren.stock.domain.actors.LocationMessages
import com.darren.stock.domain.actors.TrackedStockPotMessages
import com.darren.stock.domain.actors.UntrackedStockPotMessages
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll


object GetValue {
    suspend fun StockSystem.getValue(locationId: String, productId: String): Double {
        val stockPots = getAllStockPotAndAllChildrenForLocation(this, locationId, productId)

        return stockPots.map { sp ->
            val completable = CompletableDeferred<Double>()

            when (sp) {
                is ChannelType.TrackedChannel -> sp.channel.send(TrackedStockPotMessages.GetValue(completable))
                is ChannelType.UntrackedChannel -> sp.channel.send(UntrackedStockPotMessages.GetValue(completable))
            }
            completable
        }.awaitAll().sum()
    }

    private suspend fun getAllStockPotAndAllChildrenForLocation(
        system: StockSystem,
        locationId: String,
        productId: String
    ): Set<ChannelType> {
        val deferred = CompletableDeferred<Map<String, String>>()
        system.locations.send(LocationMessages.GetAllChildrenForParentLocation(locationId, deferred))

        val allLocations = deferred.await()
        return allStockPotsForLocations(system, locationId, allLocations, productId)
    }

    private fun allStockPotsForLocations(
        system: StockSystem, locationId: String, allLocations: Map<String, String>, productId: String
    ): Set<ChannelType> {
        val uniqueLocations = allLocations.keys.union(allLocations.values).union(setOf(locationId)).toSet()
        return uniqueLocations.mapNotNull { system.stockPots[it to productId] }.toSet()
    }
}


