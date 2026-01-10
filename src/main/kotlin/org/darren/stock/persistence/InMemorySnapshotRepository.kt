package org.darren.stock.persistence

import org.darren.stock.domain.StockState
import org.darren.stock.domain.snapshot.SnapshotRepository

class InMemorySnapshotRepository : SnapshotRepository {
    private val actorStates = mutableMapOf<String, StockState>()

    override suspend fun saveActorState(
        actorKey: String,
        state: StockState,
    ) {
        actorStates[actorKey] = state
    }

    override suspend fun loadActorState(actorKey: String): StockState? = actorStates[actorKey]

    override suspend fun hasAnySnapshots(): Boolean = actorStates.isNotEmpty()

    override suspend fun deleteAllSnapshots() {
        actorStates.clear()
    }

    override suspend fun getAllActorStates(): Map<String, StockState> {
        return actorStates.toMap() // defensive copy
    }

    override suspend fun isHealthy(): Boolean = true
}
