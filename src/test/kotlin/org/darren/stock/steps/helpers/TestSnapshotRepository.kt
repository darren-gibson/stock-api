package org.darren.stock.steps.helpers

import org.darren.stock.domain.StockState
import org.darren.stock.domain.snapshot.SnapshotRepository
import org.darren.stock.persistence.InMemorySnapshotRepository

class TestSnapshotRepository : SnapshotRepository {
    private val delegate: SnapshotRepository = InMemorySnapshotRepository()
    val savedStatesByKey = mutableMapOf<String, MutableList<StockState>>()

    override suspend fun saveActorState(
        actorKey: String,
        state: StockState,
    ) {
        delegate.saveActorState(actorKey, state)
        val statesForKey = savedStatesByKey.getOrPut(actorKey) { mutableListOf() }
        statesForKey.add(state)
    }

    override suspend fun loadActorState(actorKey: String): StockState? = delegate.loadActorState(actorKey)

    override suspend fun hasAnySnapshots(): Boolean = delegate.hasAnySnapshots()

    override suspend fun deleteAllSnapshots() = delegate.deleteAllSnapshots()

    override suspend fun getAllActorStates(): Map<String, StockState> = delegate.getAllActorStates()

    fun reset() {
        savedStatesByKey.clear()
    }
}
