package com.darren.stock.domain

import com.darren.stock.domain.actors.stockPotActor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

private const val locationId = "1"
private const val productId = "1"

fun main() = runBlocking<Unit> {
    val channel = stockPotActor()

    channel.send(StockMessages.DeliveryEvent(LocalDateTime.now(), locationId, productId, 100.0))
    channel.send(StockMessages.SaleEvent(LocalDateTime.now(), locationId, productId, 1.0))

    val deferred = CompletableDeferred<Double>()

    channel.send(StockMessages.GetValue(locationId, productId, deferred))

    println(deferred.await()) // prints "99"

    channel.close()
}