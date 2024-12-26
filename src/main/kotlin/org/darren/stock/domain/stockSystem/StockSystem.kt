package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.actors.StockPotActor.Companion.stockPotActor
import org.darren.stock.domain.actors.events.StockPotMessages
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StockSystem : KoinComponent {
//    companion object {
//        private val logger = KotlinLogging.logger {}
//    }

    val locations by inject<LocationApiClient>()
    val stockPots = mutableMapOf<Pair<String, String>, SendChannel<StockPotMessages>>()

    fun getStockPot(locationId: String, productId: String): SendChannel<StockPotMessages> {
        return stockPots.getOrPut(locationId to productId) {
            createInitialChannelType(locationId, productId, 0.0)
        }
    }

    fun setInitialStockLevel(locationId: String, productId: String, initialQuantity: Double) {
        stockPots[locationId to productId] = createInitialChannelType(locationId, productId, initialQuantity)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun createInitialChannelType(locationId: String, productId: String, initialQuantity: Double) =
        GlobalScope.stockPotActor(locationId, productId, initialQuantity)
}