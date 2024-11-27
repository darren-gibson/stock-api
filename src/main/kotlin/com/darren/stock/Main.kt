package com.darren.stock

import com.darren.stock.domain.actors.TrackedStockPotMessages
import com.darren.stock.domain.actors.TrackedStockPotActor.Companion.trackedStockPotActor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

private const val locationId = "1"
private const val productId = "1"

fun main() = runBlocking<Unit> {
    val channel = trackedStockPotActor(locationId, productId)

    channel.send(TrackedStockPotMessages.DeliveryEvent(LocalDateTime.now(), 100.0))
    channel.send(TrackedStockPotMessages.SaleEvent(LocalDateTime.now(), 1.0))

    val deferred = CompletableDeferred<Double>()

    channel.send(TrackedStockPotMessages.GetValue(deferred))

    println(deferred.await()) // prints "99"

    channel.close()
}