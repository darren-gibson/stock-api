package org.darren.stock.domain.stockSystem

import io.github.smyrgeorge.actor4k.actor.ref.ActorRef
import io.github.smyrgeorge.actor4k.system.ActorSystem
import kotlinx.coroutines.runBlocking
import org.darren.stock.domain.IdempotencyStatus
import org.darren.stock.domain.LocationApiClient
import org.darren.stock.domain.ProductLocation
import org.darren.stock.domain.actors.StockPotActor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StockSystem : KoinComponent {
    val locations by inject<LocationApiClient>()
    val eventRepository by inject<org.darren.stock.domain.StockEventRepository>()

    suspend fun getStockPot(
        locationId: String,
        productId: String,
    ): ActorRef =
        ActorSystem.get(
            StockPotActor::class,
            ProductLocation.of(productId, locationId).toString(),
        )

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
                    ActorSystem.get(
                        StockPotActor::class,
                        ProductLocation.of(productId, locationId).toString(),
                    )
            }
        }
}

suspend fun StockSystem.checkIdempotencyAndThrow(
    requestId: String,
    contentHash: String,
) {
    when (eventRepository.checkIdempotencyStatus(requestId, contentHash)) {
        IdempotencyStatus.NOT_FOUND -> {
            // Process normally - no events found
        }
        IdempotencyStatus.MATCH -> {
            return // Idempotent - request already processed successfully
        }
        IdempotencyStatus.CONTENT_MISMATCH -> {
            throw IdempotencyContentMismatchException(
                "Request with ID '$requestId' already exists but with different content",
                requestId,
            )
        }
    }
}
