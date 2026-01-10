package org.darren.stock.domain.snapshot

import org.darren.stock.domain.HealthProbe
import org.darren.stock.domain.StockState

interface SnapshotRepository : HealthProbe {
    suspend fun saveActorState(
        actorKey: String,
        state: StockState,
    )

    suspend fun loadActorState(actorKey: String): StockState?

    suspend fun hasAnySnapshots(): Boolean

    suspend fun deleteAllSnapshots()

    suspend fun getAllActorStates(): Map<String, StockState>
}
