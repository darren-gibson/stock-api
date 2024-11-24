package com.darren.stock.domain

import com.darren.stock.domain.actors.stockPotActor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

private const val locationId = "1"
private const val productId = "1"

fun main() = runBlocking<Unit> {
    val channel = stockPotActor(locationId, productId)

    channel.send(StockPotMessages.DeliveryEvent(LocalDateTime.now(), 100.0))
    channel.send(StockPotMessages.SaleEvent(LocalDateTime.now(), 1.0))

    val deferred = CompletableDeferred<Double>()

    channel.send(StockPotMessages.GetValue(deferred))

    println(deferred.await()) // prints "99"

    channel.close()
}