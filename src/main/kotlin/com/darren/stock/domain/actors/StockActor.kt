package com.darren.stock.domain.actors

import com.darren.stock.domain.StockMessages
import com.darren.stock.domain.StockMessages.*
import com.darren.stock.domain.StockPotMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.currentCoroutineContext

private val logger = KotlinLogging.logger {}

class StockActor {
    companion object {
        @OptIn(ObsoleteCoroutinesApi::class)
        fun CoroutineScope.stockActor(): SendChannel<StockMessages> = actor {
            with(StockActor()) {
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
            is SetStockLevelEvent -> initializeStockPot(message.locationId, message.productId, message.quantity)
            is GetValue -> stockPot.send(StockPotMessages.GetValue(message.deferred))
            is SaleEvent -> stockPot.send(StockPotMessages.SaleEvent(message.eventTime, message.quantity))
            is DeliveryEvent -> stockPot.send(StockPotMessages.DeliveryEvent(message.eventTime, message.quantity))
        }
    }
}