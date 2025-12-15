package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.actors.StockPotActor.Companion.createStockPotActor
import org.darren.stock.domain.actors.messages.StockPotMessages
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

class StockSystem(
    private val actorScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : KoinComponent {
    val locations by inject<LocationApiClient>()
    private val stockPots = ConcurrentHashMap<Pair<String, String>, SendChannel<StockPotMessages>>()

    fun getStockPot(
        locationId: String,
        productId: String,
    ): SendChannel<StockPotMessages> =
        stockPots.getOrPut(locationId to productId) {
            createStockPotActor(locationId, productId)
        }

    fun createStockPotActor(
        locationId: String,
        productId: String,
    ) = actorScope.createStockPotActor(locationId, productId)

    // TODO: Need to consider how to handle the case where a stock pot is no longer needed
    // TODO: Need to reduce to a number of stock pots that are actually needed.
    // What about Stores with child locations of aisles, modules, shelves, etc? The numbers will soon mount up.
    fun getAllActiveStockPotsFor(
        locationIds: Set<String>,
        productId: String,
    ): Map<String, SendChannel<StockPotMessages>> {
        val allPossible = locationIds.map { loc -> loc to productId }.toSet()
        val toCreate = allPossible - stockPots.keys

        toCreate.forEach { (locationId, productId) ->
            stockPots[locationId to productId] = createStockPotActor(locationId, productId)
        }

        return allPossible.associate { it.first to stockPots[it]!! }
    }
}
