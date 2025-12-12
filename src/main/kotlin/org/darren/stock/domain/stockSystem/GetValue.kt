package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.Location
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.LocationApiClient.LocationDTO
import org.darren.stock.domain.StockLevel
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.messages.GetValue
import org.darren.stock.domain.actors.messages.StockPotMessages
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object GetValue : KoinComponent {
    suspend fun StockSystem.getValue(
        locationId: String,
        productId: String,
        includeChildren: Boolean,
    ): StockLevel {
        val locationApi by inject<LocationApiClient>()
        val allLocations = locationApi.getLocationsHierarchy(locationId, if (includeChildren) null else 1)

        val stockPots = getAllStockPotAndAllChildrenForLocation(allLocations, productId)

        val stockCountByLocation =
            stockPots.entries.associate { sp ->
                val completable = CompletableDeferred<Reply>()
                sp.value.send(GetValue(completable))
                sp.key to completable
            }

        stockCountByLocation.values.awaitAll()

        return convertToStockLevel(allLocations, productId, stockCountByLocation)
    }

    private suspend fun convertToStockLevel(
        location: LocationDTO,
        productId: String,
        stockCountByLocation: Map<String, CompletableDeferred<Reply>>,
    ): StockLevel {
        val state =
            stockCountByLocation[location.id]?.await()?.getOrThrow() ?: StockState(
                Location(location.id),
                productId,
                null,
            )

        return StockLevel(
            state,
            location.children.map {
                convertToStockLevel(it, productId, stockCountByLocation)
            },
        )
    }

    private fun StockSystem.getAllStockPotAndAllChildrenForLocation(
        allLocations: LocationDTO,
        productId: String,
    ): Map<String, SendChannel<StockPotMessages>> {
        val locationSet = getTrackedLocationIds(allLocations).toSet()

        return getAllActiveStockPotsFor(locationSet, productId)
    }

    private fun getTrackedLocationIds(location: LocationDTO): Sequence<String> =
        sequence {
            if (location.isTracked) {
                yield(location.id)
            }
            yieldAll(location.children.flatMap(::getTrackedLocationIds))
        }
}
