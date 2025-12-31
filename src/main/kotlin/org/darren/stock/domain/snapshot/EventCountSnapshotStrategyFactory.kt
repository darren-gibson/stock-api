package org.darren.stock.domain.snapshot

import org.darren.stock.domain.snapshot.EventCountSnapshotStrategy

class EventCountSnapshotStrategyFactory(
    private val repository: SnapshotRepository,
    private val eventsPerSnapshot: Int,
) : SnapshotStrategyFactory {
    override fun createStrategy(
        actorKey: String,
    ): SnapshotStrategy =
        EventCountSnapshotStrategy(
            actorKey = actorKey,
            repository = repository,
            eventsPerSnapshot = eventsPerSnapshot,
        )
}
