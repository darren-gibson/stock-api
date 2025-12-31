package org.darren.stock.domain.snapshot

interface SnapshotStrategyFactory {
    fun createStrategy(
        actorKey: String,
    ): SnapshotStrategy
}
