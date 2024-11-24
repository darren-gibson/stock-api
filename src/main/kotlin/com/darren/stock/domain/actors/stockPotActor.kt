@file:OptIn(ObsoleteCoroutinesApi::class)

package com.darren.stock.domain.actors

import com.darren.stock.domain.StockMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor

fun CoroutineScope.stockPotActor() = actor<StockMessages> {
    var currentStock = 0.0

    for (message in channel) {
        when(message) {
            is StockMessages.DeliveryEvent -> currentStock += message.quantity
            is StockMessages.GetValue -> message.deferred.complete(currentStock)
            is StockMessages.SaleEvent -> Unit
        }
    }
}