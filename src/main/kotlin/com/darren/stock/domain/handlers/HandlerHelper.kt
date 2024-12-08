package com.darren.stock.domain.handlers

import com.darren.stock.domain.LocationType
import com.darren.stock.domain.actors.LocationMessages
import com.darren.stock.domain.actors.TrackedStockPotActor.Companion.trackedStockPotActor
import com.darren.stock.domain.actors.UntrackedStockPotActor.Companion.untrackedStockPotActor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HandlerHelper : KoinComponent {
    val locations by inject<SendChannel<LocationMessages>>()
    val stockPots = mutableMapOf<Pair<String, String>, ChannelType>()

    suspend fun getStockPot(locationId: String, productId: String): ChannelType {
        return stockPots.getOrPut(locationId to productId) {
            createInitialChannelType(locationId, productId, 0.0)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun createInitialChannelType(
        locationId: String,
        productId: String,
        initialQuantity: Double
    ) = when (getLocationType(locationId)) {
        LocationType.Tracked -> ChannelType.TrackedChannel(
            GlobalScope.trackedStockPotActor(locationId, productId, initialQuantity)
        )

        LocationType.Untracked -> ChannelType.UntrackedChannel(
            GlobalScope.untrackedStockPotActor(locationId, productId, initialQuantity)
        )
    }

    private suspend fun getLocationType(locationId: String): LocationType {
        val result = CompletableDeferred<Result<LocationType>>()
        locations.send(LocationMessages.GetLocationType(locationId, result))
        return result.await().getOrThrow()
    }
}