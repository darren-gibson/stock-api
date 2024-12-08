package com.darren.stock.domain.actors

import com.darren.stock.domain.actors.UntrackedStockPotMessages.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor


class UntrackedStockPotActor(private val locationId: String, private val productId: String, initialQuantity: Double) {
    private var currentStock = initialQuantity

    companion object {
        private val logger = KotlinLogging.logger {}
        @OptIn(ObsoleteCoroutinesApi::class)
        fun CoroutineScope.untrackedStockPotActor(locationId: String, productId: String, initialQuantity: Double = 0.0): SendChannel<UntrackedStockPotMessages> = actor {
            with(UntrackedStockPotActor(locationId, productId, initialQuantity)) {
                for (msg in channel) onReceive(msg)
            }
        }
    }

    private fun onReceive(message: UntrackedStockPotMessages) {
        logger.debug { "message received: $message" }
        when (message) {
            is GetValue -> message.response.complete(currentStock)
            is CountEvent -> currentStock = message.quantity
        }
        logger.debug { "$this" }
    }

    override fun toString(): String {
        return "UntrackedStockPotActor(locationId='$locationId', productId='$productId', currentStock=$currentStock)"
    }
}