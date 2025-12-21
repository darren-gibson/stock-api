package org.darren.stock.domain.stockSystem

import io.github.smyrgeorge.actor4k.actor.ref.ActorRef
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.darren.stock.domain.Location
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.LocationApiClient.LocationDTO
import org.darren.stock.domain.StockLevel
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.messages.StockPotProtocol
import org.darren.stock.domain.actors.messages.StockPotProtocol.GetValue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object GetValue : KoinComponent {
    suspend fun StockSystem.retrieveValue(
        locationId: String,
        productId: String,
        includeChildren: Boolean,
    ): StockLevel {
        val locationApi by inject<LocationApiClient>()
        val allLocations = locationApi.getLocationsHierarchy(locationId, if (includeChildren) null else 1)

        val stockPots = getAllStockPotAndAllChildrenForLocation(allLocations, productId)
        val stockCountByLocation =
            coroutineScope {
                stockPots.entries
                    .map { sp ->
                        async {
                            val reply = sp.value.ask(GetValue())
                            sp.key to reply
                        }
                    }.awaitAll()
                    .toMap()
            }

        return convertToStockLevel(allLocations, productId, stockCountByLocation)
    }

    private fun convertToStockLevel(
        location: LocationDTO,
        productId: String,
        stockCountByLocation: Map<String, Result<StockPotProtocol.Reply>>,
    ): StockLevel {
        val state: StockState =
            stockCountByLocation[location.id]?.getOrThrow()?.result ?: StockState(
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
    ): Map<String, ActorRef> {
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
