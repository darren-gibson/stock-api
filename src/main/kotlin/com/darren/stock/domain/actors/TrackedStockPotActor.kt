package com.darren.stock.domain.actors

import com.darren.stock.domain.actors.TrackedStockPotMessages.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor


class TrackedStockPotActor(private val locationId: String, private val productId: String, initialQuantity: Double) {
    private var currentStock = initialQuantity

    companion object {
        private val logger = KotlinLogging.logger {}

        @OptIn(ObsoleteCoroutinesApi::class)
        fun CoroutineScope.trackedStockPotActor(locationId: String, productId: String, initialQuantity: Double = 0.0): SendChannel<TrackedStockPotMessages> = actor {
            with(TrackedStockPotActor(locationId, productId, initialQuantity)) {
                for (msg in channel) onReceive(msg)
            }
        }
    }

    private fun onReceive(message: TrackedStockPotMessages) {
        logger.debug { "message received: $message" }
        when (message) {
            is GetValue -> message.response.complete(currentStock)
            is SaleEvent -> currentStock -= message.quantity
            is DeliveryEvent -> currentStock += message.quantity
        }
        logger.debug { "$this" }
    }

    override fun toString(): String {
        return "TrackedStockPotActor(locationId='$locationId', productId='$productId', currentStock=$currentStock)"
    }
}