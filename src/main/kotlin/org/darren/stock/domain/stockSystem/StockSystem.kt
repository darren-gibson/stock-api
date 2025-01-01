package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.actors.StockPotActor.Companion.stockPotActor
import org.darren.stock.domain.actors.messages.StockPotMessages
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StockSystem : KoinComponent {
    val locations by inject<LocationApiClient>()
    val stockPots = mutableMapOf<Pair<String, String>, SendChannel<StockPotMessages>>()

    fun getStockPot(locationId: String, productId: String): SendChannel<StockPotMessages> {
        return stockPots.getOrPut(locationId to productId) {
            createInitialChannelType(locationId, productId)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun createInitialChannelType(locationId: String, productId: String) =
        GlobalScope.stockPotActor(locationId, productId)
}