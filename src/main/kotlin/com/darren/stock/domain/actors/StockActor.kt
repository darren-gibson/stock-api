package com.darren.stock.domain.actors

import com.darren.stock.domain.LocationMessages
import com.darren.stock.domain.StockMessages
import com.darren.stock.domain.StockMessages.*
import com.darren.stock.domain.StockPotMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor

class StockActor(private val locations: SendChannel<LocationMessages>) {
    companion object {
        private val logger = KotlinLogging.logger {}

        @OptIn(ObsoleteCoroutinesApi::class)
        fun CoroutineScope.stockActor(locations: SendChannel<LocationMessages>): SendChannel<StockMessages> = actor {
            with(StockActor(locations)) {
                for (msg in channel) onReceive(msg)
            }
        }
    }

    private val stockPots = mutableMapOf<Pair<String, String>, SendChannel<StockPotMessages>>()
    private suspend fun getStockPot(locationId: String, productId: String) =
        stockPots.getOrPut(locationId to productId) {
            with(CoroutineScope(currentCoroutineContext())) {
                stockPotActor(locationId, productId, 0.0)
            }
        }

    private suspend fun initializeStockPot(locationId: String, productId: String, initialQuantity: Double) {
        stockPots[locationId to productId] =
            CoroutineScope(currentCoroutineContext()).stockPotActor(locationId, productId, initialQuantity)
    }

    suspend fun onReceive(message: StockMessages) {
        logger.debug { "message received: $message" }
        val stockPot = getStockPot(message.locationId, message.productId)
        when (message) {
            is GetValue -> calculateStock(message)
            is SetStockLevelEvent -> initializeStockPot(message.locationId, message.productId, message.quantity)
            is SaleEvent -> stockPot.send(StockPotMessages.SaleEvent(message.eventTime, message.quantity))
            is DeliveryEvent -> stockPot.send(StockPotMessages.DeliveryEvent(message.eventTime, message.quantity))
        }
    }

    private suspend fun calculateStock(message: GetValue) {
        val stockPots = getAllStockPotAndAllChildrenForLocation(message.locationId, message.productId)

        val result = stockPots.map { sp ->
            val completable = CompletableDeferred<Double>()
            sp.send(StockPotMessages.GetValue(completable))
            completable
        }.awaitAll().sum()

        message.deferred.complete(result)
    }

    private suspend fun getAllStockPotAndAllChildrenForLocation(
        locationId: String,
        productId: String
    ): Set<SendChannel<StockPotMessages>> {
        val deferred = CompletableDeferred<Map<String, String>>()
        locations.send(LocationMessages.GetAllChildrenForParentLocation(locationId, deferred))

        val allLocations = deferred.await()
        return allStockPotsForLocations(locationId, allLocations, productId)
    }

    private fun allStockPotsForLocations(
        locationId: String,
        allLocations: Map<String, String>,
        productId: String
    ): Set<SendChannel<StockPotMessages>> {
        val uniqueLocations = allLocations.keys.union(allLocations.values).union(setOf(locationId)).toSet()
        return uniqueLocations.mapNotNull { stockPots[it to productId] }.toSet()
    }
}