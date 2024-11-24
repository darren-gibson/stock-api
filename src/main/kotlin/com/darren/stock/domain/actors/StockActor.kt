package com.darren.stock.domain.actors

import com.darren.stock.domain.StockMessages
import com.darren.stock.domain.StockMessages.*
import com.darren.stock.domain.StockPotMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

@OptIn(ObsoleteCoroutinesApi::class)
fun CoroutineScope.stockActor() = actor<StockMessages> {
    val stockPot = stockPotActor("", "", 0.0)
    for (message in channel) {
        logger.debug { "message received: $message" }
        when (message) {
            is GetValue -> stockPot.send(StockPotMessages.GetValue(message.deferred))
            is SaleEvent -> Unit
            is DeliveryEvent -> stockPot.send(StockPotMessages.DeliveryEvent(message.eventTime, message.quantity))
        }
    }
}