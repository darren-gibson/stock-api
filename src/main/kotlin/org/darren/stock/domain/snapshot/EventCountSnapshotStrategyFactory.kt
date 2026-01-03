package org.darren.stock.domain.snapshot

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
