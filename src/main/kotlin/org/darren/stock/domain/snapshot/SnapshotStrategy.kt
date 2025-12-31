package org.darren.stock.domain.snapshot

import io.github.oshai.kotlinlogging.KotlinLogging
import org.darren.stock.domain.StockState

abstract class SnapshotStrategy(
    private val actorKey: String,
    private val repository: SnapshotRepository,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Called when an event has been processed.
     */
    abstract suspend fun onEventProcessed(currentState: StockState)

    /**
     * Loads snapshot data for the actor.
     */
    suspend fun loadSnapshotData(actorKey: String): StockState? = repository.loadActorState(actorKey)

    protected suspend fun takeSnapshot(
        currentState: StockState,
    ) {
        repository.saveActorState(actorKey, currentState)
        logger.debug { "Saved snapshot for actor $actorKey after state: $currentState" }
    }
}
