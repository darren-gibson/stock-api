@file:OptIn(ObsoleteCoroutinesApi::class)

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
    private val stockPots = mutableMapOf<Pair<String, String>, SendChannel<StockPotMessages>>()
    private suspend fun getStockPot(locationId: String, productId: String) =
        stockPots.getOrPut(locationId to productId) {
            with(CoroutineScope(currentCoroutineContext())) {
                stockPotActor(locationId, productId, 0.0)
            }
        }

    suspend fun onReceive(message: StockMessages) {
        logger.debug { "message received: $message" }
        val stockPot = getStockPot(message.locationId, message.productId)
        when (message) {
            is GetValue -> stockPot.send(StockPotMessages.GetValue(message.deferred))
            is SaleEvent -> Unit
            is DeliveryEvent -> stockPot.send(StockPotMessages.DeliveryEvent(message.eventTime, message.quantity))
        }
    }
}

fun CoroutineScope.stockActor(): SendChannel<StockMessages> = actor {
    with(StockActor()) {
        for (msg in channel) onReceive(msg)
    }
}