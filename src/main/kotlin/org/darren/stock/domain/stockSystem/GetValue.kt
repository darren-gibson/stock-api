package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.actors.events.StockPotMessages
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.Location
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.StockLevel
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.events.GetValue
import org.darren.stock.domain.actors.Reply
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object GetValue : KoinComponent {
    suspend fun StockSystem.getValue(locationId: String, productId: String, includeChildren: Boolean): StockLevel {
        val locationApi by inject<LocationApiClient>()
        val allLocations = locationApi.getLocationsHierarchy(locationId, if(includeChildren) null else 1)

        val stockPots = getAllStockPotAndAllChildrenForLocation(allLocations, productId)

        val stockCountByLocation = stockPots.entries.associate { sp ->
            val completable = CompletableDeferred<Reply>()
            sp.value.send(GetValue(completable))
            sp.key to completable
        }

        stockCountByLocation.values.awaitAll()

        return convertToStockLevel(allLocations, productId, stockCountByLocation)
    }

    private suspend fun convertToStockLevel(
        location: LocationApiClient.LocationDTO,
        productId: String,
        stockCountByLocation: Map<String, CompletableDeferred<Reply>>
    ): StockLevel {
        val state = stockCountByLocation[location.id]?.await()?.getOrThrow() ?:
            StockState(Location(location.id), productId, 0.0)

        val stockLevel = StockLevel(state, location.children.map {
            convertToStockLevel(it, productId, stockCountByLocation)
        })
        return stockLevel
    }

    private fun StockSystem.getAllStockPotAndAllChildrenForLocation(
        allLocations: LocationApiClient.LocationDTO,
        productId: String
    ): Map<String, SendChannel<StockPotMessages>> {
        val locationSet = extractAllLocationIds(allLocations).toSet()

        return allStockPotsForLocations(locationSet, productId)
    }

//    private fun getAllLocations(location: LocationApiClient.LocationDTO): Set<String> =
//        setOf(location.id).plus(location.children.flatMap(::getAllLocations))

    private fun extractAllLocationIds(location: LocationApiClient.LocationDTO): Sequence<String> = sequence {
        yield(location.id)
        yieldAll(location.children.flatMap(::extractAllLocationIds))
    }

    private fun StockSystem.allStockPotsForLocations(allLocations: Set<String>, productId: String) =
        allLocations.associateWithNotNull { stockPots[it to productId] }

    inline fun <K, V> Set<K>.associateWithNotNull(transform: (K) -> V?): Map<K, V> {
        return buildMap {
            for (key in this@associateWithNotNull) {
                transform(key)?.let { value -> put(key, value) }
            }
        }
    }
}


