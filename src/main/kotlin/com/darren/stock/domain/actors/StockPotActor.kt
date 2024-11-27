package com.darren.stock.domain.actors

import com.darren.stock.domain.StockPotMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor


class StockPotActor(val locationId: String, val productId: String, initialQuantity: Double) {
    private var currentStock = initialQuantity

    companion object {
        private val logger = KotlinLogging.logger {}

        @OptIn(ObsoleteCoroutinesApi::class)
        fun CoroutineScope.stockPotActor(locationId: String, productId: String, initialQuantity: Double = 0.0): SendChannel<StockPotMessages> = actor {
            with(StockPotActor(locationId, productId, initialQuantity)) {
                for (msg in channel) onReceive(msg)
            }
        }
    }

    private fun onReceive(message: StockPotMessages) {
        logger.debug { "message received: $message" }
        when (message) {
            is StockPotMessages.GetValue -> message.response.complete(currentStock)
            is StockPotMessages.SaleEvent -> currentStock -= message.quantity
            is StockPotMessages.DeliveryEvent -> currentStock += message.quantity
        }
        logger.debug { "$this" }
    }

    override fun toString(): String {
        return "StockPotActor(locationId='$locationId', productId='$productId', currentStock=$currentStock)"
    }
}