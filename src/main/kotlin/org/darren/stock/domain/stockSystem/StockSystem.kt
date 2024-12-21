package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.actors.TrackedStockPotActor.Companion.trackedStockPotActor
import org.darren.stock.domain.actors.UntrackedStockPotActor.Companion.untrackedStockPotActor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StockSystem : KoinComponent {
//    companion object {
//        private val logger = KotlinLogging.logger {}
//    }

    val locations by inject<LocationApiClient>()
    val stockPots = mutableMapOf<Pair<String, String>, ChannelType>()

    suspend fun getStockPot(locationId: String, productId: String): ChannelType {
        return stockPots.getOrPut(locationId to productId) {
            createInitialChannelType(locationId, productId, 0.0)
        }
    }

    suspend fun setInitialStockLevel(locationId: String, productId: String, initialQuantity: Double) {
        stockPots[locationId to productId] = createInitialChannelType(locationId, productId, initialQuantity)
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun createInitialChannelType(
        locationId: String,
        productId: String,
        initialQuantity: Double
    ) =
        if (getLocationType(locationId))
            ChannelType.TrackedChannel(GlobalScope.trackedStockPotActor(locationId, productId, initialQuantity))
        else ChannelType.UntrackedChannel(GlobalScope.untrackedStockPotActor(locationId, productId, initialQuantity))

    private suspend fun getLocationType(locationId: String) = locations.isTracked(locationId)
}