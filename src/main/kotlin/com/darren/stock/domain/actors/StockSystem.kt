package com.darren.stock.domain.actors

import com.darren.stock.domain.LocationMessages
import com.darren.stock.domain.StockPotMessages
import com.darren.stock.domain.actors.StockPotActor.Companion.stockPotActor
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

    private val stockPots = mutableMapOf<Pair<String, String>, SendChannel<StockPotMessages>>()

    @OptIn(DelicateCoroutinesApi::class)
    private fun getStockPot(locationId: String, productId: String) =
        stockPots.getOrPut(locationId to productId) {
            with(GlobalScope) {
                stockPotActor(locationId, productId, 0.0)
            }
        }

    suspend fun getValue(locationId: String, productId: String): Double {
        val stockPots = getAllStockPotAndAllChildrenForLocation(locationId, productId)

        return stockPots.map { sp ->
            val completable = CompletableDeferred<Double>()
            sp.send(StockPotMessages.GetValue(completable))
            completable
        }.awaitAll().sum()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun setStockLevel(locationId: String, productId: String, initialQuantity: Double) {
        stockPots[locationId to productId] = GlobalScope.stockPotActor(locationId, productId, initialQuantity)
    }

    suspend fun sale(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
        getStockPot(locationId, productId).send(StockPotMessages.SaleEvent(eventTime, quantity))
    }

    suspend fun delivery(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
        getStockPot(locationId, productId).send(StockPotMessages.DeliveryEvent(eventTime, quantity))
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