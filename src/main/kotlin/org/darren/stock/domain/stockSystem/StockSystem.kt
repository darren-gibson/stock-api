package org.darren.stock.domain.stockSystem

import arrow.resilience.Schedule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.actor4k.actor.ref.ActorRef
import io.github.smyrgeorge.actor4k.system.ActorSystem
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
        val logger = KotlinLogging.logger { }
        val key = ProductLocation.of(productId, locationId).toString()
        logger.debug { "running getStockPot for $key" }
        return activeStockPots.getOrPut(key) {
            logger.debug { "calling the ActorSystem to get Actor for $key" }
            ActorSystem.get(
                StockPotActor::class,
                key,
            )
        }
    }

    // TODO: Need to consider how to handle the case where a stock pot is no longer needed
    // TODO: Need to reduce to a number of stock pots that are actually needed.
    // What about Stores with child locations of aisles, modules, shelves, etc? The numbers will
    // soon mount up.
    // TODO: Should not be blocking here
    suspend fun getAllActiveStockPotsFor(
        locationIds: Set<String>,
        productId: String,
    ): Map<String, ActorRef> {
        val allPossible = locationIds.map { loc -> loc to productId }.toSet()

        return allPossible.associate { (locationId, productId) ->
            locationId to
                getStockPot(locationId, productId)
        }
    }
}
