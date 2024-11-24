package com.darren.stock.domain.actors

import com.darren.stock.domain.StockPotMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}
@OptIn(ObsoleteCoroutinesApi::class)
fun CoroutineScope.stockPotActor(locationId: String, productId: String, initialQuantity: Double = 0.0) =
    actor<StockPotMessages> {
        var currentStock = initialQuantity

        for (message in channel) {
            logger.debug { "message received: $message" }
            when (message) {
                is StockPotMessages.GetValue -> message.deferred.complete(currentStock)
                is StockPotMessages.SaleEvent -> currentStock -= message.quantity
                is StockPotMessages.DeliveryEvent -> currentStock += message.quantity
            }
        }
    }