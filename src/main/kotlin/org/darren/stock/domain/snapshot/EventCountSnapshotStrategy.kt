package org.darren.stock.domain.snapshot

import org.darren.stock.domain.StockState

/**
 * A snapshot strategy that takes a snapshot every N events.
 */
class EventCountSnapshotStrategy(
    actorKey: String,
    repository: SnapshotRepository,
    private val eventsPerSnapshot: Int,
) : SnapshotStrategy(actorKey, repository) {
    private var eventCount: Int = 0

    init {
        require(eventsPerSnapshot > 0) { "eventsPerSnapshot must be positive" }
    }

    fun shouldTakeSnapshot(): Boolean = eventCount % eventsPerSnapshot == 0

    override suspend fun onEventProcessed(currentState: StockState) {
        eventCount++
        if (shouldTakeSnapshot()) {
            takeSnapshot(currentState)
        }
    }
}
