package com.darren.stock.domain

import com.darren.stock.domain.actors.LocationMessages
import com.darren.stock.domain.actors.TrackedStockPotActor.Companion.trackedStockPotActor
import com.darren.stock.domain.actors.UntrackedStockPotActor.Companion.untrackedStockPotActor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.SendChannel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime
import com.darren.stock.domain.actors.TrackedStockPotMessages as TSPM
import com.darren.stock.domain.actors.UntrackedStockPotMessages as USPM

class StockSystem : KoinComponent {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val locations by inject<SendChannel<LocationMessages>>()
    private val stockPots = mutableMapOf<Pair<String, String>, ChannelType>()

    sealed class ChannelType {
        class TrackedChannel(val channel: SendChannel<TSPM>) : ChannelType()
        class UntrackedChannel(val channel: SendChannel<USPM>) : ChannelType()
    }

    private suspend fun getStockPot(locationId: String, productId: String): ChannelType {
        return stockPots.getOrPut(locationId to productId) {
            createInitialChannelType(locationId, productId, 0.0)
        }
    }

    private suspend fun getLocationType(locationId: String): LocationType {
        val result = CompletableDeferred<Result<LocationType>>()
        locations.send(LocationMessages.GetLocationType(locationId, result))
        return result.await().getOrThrow()
    }

    suspend fun getValue(locationId: String, productId: String): Double {
        val stockPots = getAllStockPotAndAllChildrenForLocation(locationId, productId)

        return stockPots.map { sp ->
            val completable = CompletableDeferred<Double>()

            when (sp) {
                is ChannelType.TrackedChannel -> sp.channel.send(TSPM.GetValue(completable))
                is ChannelType.UntrackedChannel -> sp.channel.send(USPM.GetValue(completable))
            }
            completable
        }.awaitAll().sum()
    }

    suspend fun setInitialStockLevel(locationId: String, productId: String, initialQuantity: Double) {
        stockPots[locationId to productId] = createInitialChannelType(locationId, productId, initialQuantity)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun createInitialChannelType(
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

    suspend fun count(
        location: String,
        product: String,
        quantity: Double,
        reason: StockCountReason,
        eventTime: LocalDateTime
    ) {
        when (val type = getStockPot(location, product)) {
            is ChannelType.TrackedChannel -> type.channel.send(TSPM.CountEvent(eventTime, quantity, reason))
            is ChannelType.UntrackedChannel -> type.channel.send(USPM.CountEvent(eventTime, quantity, reason))
        }
    }

    suspend fun sale(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
        when (val type = getStockPot(locationId, productId)) {
            is ChannelType.TrackedChannel -> type.channel.send(TSPM.SaleEvent(eventTime, quantity))
            else -> throw OperationNotSupportedException("Untracked location $locationId cannot perform sales.")
        }
    }

    suspend fun delivery(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
        when (val type = getStockPot(locationId, productId)) {
            is ChannelType.TrackedChannel -> type.channel.send(
                TSPM.DeliveryEvent(eventTime, quantity)
            )

            else -> throw OperationNotSupportedException("Untracked location $locationId cannot accept deliveries.")
        }
    }

    private suspend fun getAllStockPotAndAllChildrenForLocation(
        locationId: String,
        productId: String
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

    suspend fun move(movement: StockMovement) {
        val from = getStockPot(movement.from, movement.product)
        val to = getStockPot(movement.to, movement.product)

        if (from is ChannelType.TrackedChannel && to is ChannelType.TrackedChannel) {
            move(movement, from, to)
        } else {
            throw OperationNotSupportedException("both ${movement.from} and ${movement.to} must be Tracked locations.")
        }
    }

    private suspend fun move(move: StockMovement, from: ChannelType.TrackedChannel, to: ChannelType.TrackedChannel) {
        val result = CompletableDeferred<MoveResult>()

        with(move) {
            from.channel.send(TSPM.MoveEvent(product, quantity, to.channel, reason, LocalDateTime.now(), result))
            when (result.await()) {
                MoveResult.Success -> return
                MoveResult.InsufficientStock -> throw InsufficientStockException()
            }
        }
    }
}