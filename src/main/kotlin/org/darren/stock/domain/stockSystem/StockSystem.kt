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

/**
 * Central registry for stock pot actors. Delegates actor lifecycle management to Actor4k's
 * ActorSystem, which handles creation, caching, and automatic eviction of inactive actors.
 *
 * Actor lifecycle is configured via Actor4k settings:
 * - `actorExpiresAfter`: Duration of inactivity before an actor is evicted from memory
 * - `registryCleanupEvery`: Frequency of cleanup checks for expired actors
 *
 * Actors are automatically rehydrated from persisted event streams when accessed after eviction.
 */
class StockSystem : KoinComponent {
    val locations by inject<LocationApiClient>()
    val retryPolicy =
        Schedule.exponential<RetriableException>(100.milliseconds)

    /**
     * Retrieves or creates a StockPotActor for the given location and product.
     * Actor4k ensures thread-safe, idempotent actor creation and manages the actor lifecycle.
     *
     * @param locationId The location identifier
     * @param productId The product identifier
     * @return ActorRef for the stock pot actor
     */
    suspend fun getStockPot(
        locationId: String,
        productId: String,
    ): ActorRef {
        val logger = KotlinLogging.logger { }
        val key = ProductLocation.of(productId, locationId).toString()
        logger.debug { "running getStockPot for $key" }
        return ActorSystem.get(
            StockPotActor::class,
            key,
        )
    }

    /**
     * Retrieves stock pot actors for all specified locations and a single product.
     * Used for hierarchical stock queries where child location stock needs to be aggregated.
     *
     * Note: Actor4k automatically manages memory for inactive actors based on configured
     * eviction policies. Large location hierarchies are handled efficiently through
     * automatic actor lifecycle management.
     *
     * @param locationIds Set of location identifiers
     * @param productId The product identifier
     * @return Map of location ID to ActorRef
     */
    suspend fun getAllActiveStockPotsFor(
        locationIds: Set<String>,
        productId: String,
    ): Map<String, ActorRef> =
        kotlinx.coroutines.coroutineScope {
            locationIds
                .associate { locationId ->
                    locationId to getStockPot(locationId, productId)
                }
        }
}
