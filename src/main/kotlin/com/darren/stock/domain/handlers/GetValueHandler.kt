package com.darren.stock.domain.handlers

import com.darren.stock.domain.actors.LocationMessages
import com.darren.stock.domain.actors.TrackedStockPotMessages
import com.darren.stock.domain.actors.UntrackedStockPotMessages
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll

class GetValueHandler (private val helper: HandlerHelper) {
    suspend fun getValue(locationId: String, productId: String): Double {
        val stockPots = getAllStockPotAndAllChildrenForLocation(locationId, productId)

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
        locationId: String,
        productId: String
    ): Set<ChannelType> {
        val deferred = CompletableDeferred<Map<String, String>>()
        helper.locations.send(LocationMessages.GetAllChildrenForParentLocation(locationId, deferred))

        val allLocations = deferred.await()
        return allStockPotsForLocations(locationId, allLocations, productId)
    }

    private fun allStockPotsForLocations(
        locationId: String, allLocations: Map<String, String>, productId: String
    ): Set<ChannelType> {
        val uniqueLocations = allLocations.keys.union(allLocations.values).union(setOf(locationId)).toSet()
        return uniqueLocations.mapNotNull { helper.stockPots[it to productId] }.toSet()
    }
}