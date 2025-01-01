package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.actors.StockPotActor.Companion.stockPotActor
import org.darren.stock.domain.actors.messages.StockPotMessages
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// TODO: This needs to be thread safe
class StockSystem : KoinComponent {
    val locations by inject<LocationApiClient>()
    private val stockPots = mutableMapOf<Pair<String, String>, SendChannel<StockPotMessages>>()

    fun getStockPot(locationId: String, productId: String): SendChannel<StockPotMessages> {
        return stockPots.getOrPut(locationId to productId) {
            createStockPotActor(locationId, productId)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun createStockPotActor(locationId: String, productId: String) =
        GlobalScope.stockPotActor(locationId, productId)

    // TODO: Neet to consider how to handle the case where a stock pot is no longer needed
    fun getAllActiveStockPotsFor(locationIds: Set<String>, productId: String):
            Map<String, SendChannel<StockPotMessages>>{
        val allPossible = locationIds.map { loc -> loc to productId }.toSet()
        val toCreate = allPossible - stockPots.keys

        toCreate.forEach { (locationId, productId) ->
            stockPots[locationId to productId] = createStockPotActor(locationId, productId)
        }

        return allPossible.associate { it.first to stockPots[it]!! }
    }
}