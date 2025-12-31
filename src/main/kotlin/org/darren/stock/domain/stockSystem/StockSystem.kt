package org.darren.stock.domain.stockSystem

import arrow.resilience.Schedule
import io.github.smyrgeorge.actor4k.actor.ref.ActorRef
import io.github.smyrgeorge.actor4k.system.ActorSystem
import kotlinx.coroutines.runBlocking
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.ProductLocation
import org.darren.stock.domain.RetriableException
import org.darren.stock.domain.actors.StockPotActor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.milliseconds

class StockSystem : KoinComponent {
    val locations by inject<LocationApiClient>()
    val retryPolicy =
        Schedule.exponential<RetriableException>(100.milliseconds)

    // Track active stock pots for snapshotting
    private val activeStockPots = mutableMapOf<String, ActorRef>()

    suspend fun getStockPot(
        locationId: String,
        productId: String,
    ): ActorRef {
        val key = ProductLocation.of(productId, locationId).toString()
        return activeStockPots.getOrPut(key) {
            ActorSystem.get(
                StockPotActor::class,
                key,
            )
        }
    }

    fun getAllActiveStockPots(): Map<String, ActorRef> = activeStockPots.toMap()

    // TODO: Need to consider how to handle the case where a stock pot is no longer needed
    // TODO: Need to reduce to a number of stock pots that are actually needed.
    // What about Stores with child locations of aisles, modules, shelves, etc? The numbers will
    // soon mount up.
    // TODO: Should not be blocking here
    fun getAllActiveStockPotsFor(
        locationIds: Set<String>,
        productId: String,
    ): Map<String, ActorRef> =
        runBlocking {
            val allPossible = locationIds.map { loc -> loc to productId }.toSet()

            allPossible.associate { (locationId, productId) ->
                locationId to
                    getStockPot(locationId, productId)
            }
        }
}
