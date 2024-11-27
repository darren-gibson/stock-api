package com.darren.stock.domain

import com.darren.stock.domain.actors.TrackedStockPotActor.Companion.trackedStockPotActor
import com.darren.stock.domain.actors.TrackedStockPotMessages
import com.darren.stock.domain.actors.UntrackedStockPotActor.Companion.untrackedStockPotActor
import com.darren.stock.domain.actors.UntrackedStockPotMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.SendChannel
import java.time.LocalDateTime

class StockSystem(private val locations: SendChannel<LocationMessages>) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    sealed class ChannelType {
        class TrackedChannel(val channel: SendChannel<TrackedStockPotMessages>) : ChannelType()
        class UntrackedChannel(val channel: SendChannel<UntrackedStockPotMessages>) : ChannelType()
    }

    private val stockPots = mutableMapOf<Pair<String, String>, ChannelType>()

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun getStockPot(locationId: String, productId: String) =
        stockPots.getOrPut(locationId to productId) {
            with(GlobalScope) {
                when (getLocationType(locationId)) {
                    LocationType.Tracked -> ChannelType.TrackedChannel(trackedStockPotActor(locationId, productId, 0.0))
                    LocationType.Untracked -> ChannelType.UntrackedChannel(
                        untrackedStockPotActor(
                            locationId,
                            productId,
                            0.0
                        )
                    )
                }
            }
        }

    private suspend fun getLocationType(locationId: String): LocationType {
        val result = CompletableDeferred<LocationType>()
        locations.send(LocationMessages.GetLocationType(locationId, result))
        return result.await()
    }

    suspend fun getValue(locationId: String, productId: String): Double {
        val stockPots = getAllStockPotAndAllChildrenForLocation(locationId, productId)

        return stockPots.map { sp ->
            val completable = CompletableDeferred<Double>()

            when (sp) {
                is ChannelType.TrackedChannel -> sp.channel.send(TrackedStockPotMessages.GetValue(completable))
                is ChannelType.UntrackedChannel -> sp.channel.send(UntrackedStockPotMessages.GetValue(completable))
            }
            completable
        }.awaitAll().sum()
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun setStockLevel(locationId: String, productId: String, initialQuantity: Double) {
        when (getStockPot(locationId, productId)) {
            is ChannelType.TrackedChannel -> stockPots[locationId to productId] =
                ChannelType.TrackedChannel(GlobalScope.trackedStockPotActor(locationId, productId, initialQuantity))

            is ChannelType.UntrackedChannel -> stockPots[locationId to productId] =
                ChannelType.UntrackedChannel(GlobalScope.untrackedStockPotActor(locationId, productId, initialQuantity))
        }
    }

    suspend fun sale(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
        when (val type = getStockPot(locationId, productId)) {
            is ChannelType.TrackedChannel -> type.channel.send(TrackedStockPotMessages.SaleEvent(eventTime, quantity))
            else -> throw OperationNotSupportedException("Untracked location $locationId cannot perform sales.")
        }
    }

    suspend fun delivery(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
        when (val type = getStockPot(locationId, productId)) {
            is ChannelType.TrackedChannel -> type.channel.send(
                TrackedStockPotMessages.DeliveryEvent(eventTime, quantity)
            )

            else -> throw OperationNotSupportedException("Untracked location $locationId cannot accept deliveries.")
        }
    }

    private suspend fun getAllStockPotAndAllChildrenForLocation(
        locationId: String, productId: String
    ): Set<ChannelType> {
        val deferred = CompletableDeferred<Map<String, String>>()
        locations.send(LocationMessages.GetAllChildrenForParentLocation(locationId, deferred))

        val allLocations = deferred.await()
        return allStockPotsForLocations(locationId, allLocations, productId)
    }

    private fun allStockPotsForLocations(
        locationId: String, allLocations: Map<String, String>, productId: String
    ): Set<ChannelType> {
        val uniqueLocations = allLocations.keys.union(allLocations.values).union(setOf(locationId)).toSet()
        return uniqueLocations.mapNotNull { stockPots[it to productId] }.toSet()
    }
}