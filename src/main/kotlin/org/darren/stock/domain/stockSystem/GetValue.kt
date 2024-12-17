package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.actors.LocationMessages
import org.darren.stock.domain.actors.TrackedStockPotMessages
import org.darren.stock.domain.actors.UntrackedStockPotMessages
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll


object GetValue {
    suspend fun org.darren.stock.domain.stockSystem.StockSystem.getValue(locationId: String, productId: String): Double {
        val stockPots =
            _root_ide_package_.org.darren.stock.domain.stockSystem.GetValue.getAllStockPotAndAllChildrenForLocation(
                this,
                locationId,
                productId
            )

        return stockPots.map { sp ->
            val completable = CompletableDeferred<Double>()

            when (sp) {
                is org.darren.stock.domain.stockSystem.ChannelType.TrackedChannel -> sp.channel.send(TrackedStockPotMessages.GetValue(completable))
                is _root_ide_package_.org.darren.stock.domain.stockSystem.ChannelType.UntrackedChannel -> sp.channel.send(UntrackedStockPotMessages.GetValue(completable))
            }
            completable
        }.awaitAll().sum()
    }

    private suspend fun getAllStockPotAndAllChildrenForLocation(
        system: _root_ide_package_.org.darren.stock.domain.stockSystem.StockSystem,
        locationId: String,
        productId: String
    ): Set<_root_ide_package_.org.darren.stock.domain.stockSystem.ChannelType> {
        val deferred = CompletableDeferred<Map<String, String>>()
        system.locations.send(LocationMessages.GetAllChildrenForParentLocation(locationId, deferred))

        val allLocations = deferred.await()
        return _root_ide_package_.org.darren.stock.domain.stockSystem.GetValue.allStockPotsForLocations(
            system,
            locationId,
            allLocations,
            productId
        )
    }

    private fun allStockPotsForLocations(
        system: _root_ide_package_.org.darren.stock.domain.stockSystem.StockSystem, locationId: String, allLocations: Map<String, String>, productId: String
    ): Set<_root_ide_package_.org.darren.stock.domain.stockSystem.ChannelType> {
        val uniqueLocations = allLocations.keys.union(allLocations.values).union(setOf(locationId)).toSet()
        return uniqueLocations.mapNotNull { system.stockPots[it to productId] }.toSet()
    }
}


