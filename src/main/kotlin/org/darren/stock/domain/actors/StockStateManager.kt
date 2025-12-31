package org.darren.stock.domain.actors

import io.github.oshai.kotlinlogging.KotlinLogging
import org.darren.stock.domain.*
import org.darren.stock.domain.ProductLocation
import org.darren.stock.domain.actors.events.StockPotEvent
import org.darren.stock.domain.snapshot.SnapshotStrategyFactory
import org.darren.stock.util.LoggingHelper.logOperation
import java.time.LocalDateTime

class StockStateManager(
    private val locationId: String,
    private val productId: String,
    private val repository: StockEventRepository,
    snapshotStrategyFactory: SnapshotStrategyFactory,
) {
    private val logger = KotlinLogging.logger {}
    private val actorKey = ProductLocation.of(productId, locationId).toString()
    private val snapshotStrategy = snapshotStrategyFactory.createStrategy(actorKey)
    var currentState = StockState(Location(locationId), productId)
        private set
    var lastEventTime: LocalDateTime? = null
        private set
    var lastRequestId: String? = null
        private set

    suspend fun initializeState() {
        val actorKey = ProductLocation.of(productId, locationId).toString()
        val snapshotState = snapshotStrategy.loadSnapshotData(actorKey)
        if (snapshotState != null) {
            currentState = snapshotState
            logger.debug { "Loaded state from snapshot: $currentState, lastEventTime: $lastEventTime, lastRequestId: $lastRequestId" }

            // Replay any events that occurred after the snapshot
            replayEventsAfterSnapshot(snapshotState.lastRequestId!!)
        } else {
            logger.debug { "No valid snapshot found (snapshotData=$snapshotState), recreating state from events" }
            recreateStateFromEvents()
        }
    }

    private suspend fun replayEventsAfterSnapshot(lastRequestId: String) {
        val eventsAfterSnapshot =
            repository.getEventsAfterRequestIdInChronologicalOrder(locationId, productId, lastRequestId)
        val eventCount = processEvents(eventsAfterSnapshot)
        logger.debug { "Replayed $eventCount events that occurred after snapshot with lastRequestId $lastRequestId" }
        if (eventCount > 0) {
            logger.debug { "State after replaying post-snapshot events: $currentState" }
        }
    }

    suspend fun processEvent(event: StockPotEvent): StockState {
        persistEvent(event)

        return if (isEventOutOfOrder(event)) {
            handleOutOfOrderEvent(event)
        } else {
            handleInOrderEvent(event)
        }
    }

    private fun isEventOutOfOrder(event: StockPotEvent): Boolean = lastEventTime != null && event.eventDateTime < lastEventTime

    private suspend fun handleOutOfOrderEvent(event: StockPotEvent): StockState {
        logger.debug {
            "Event $event is out of order (lastEventTime=$lastEventTime, eventTime=${event.eventDateTime}), rebuilding state"
        }
        return recreateStateFromEvents()
    }

    private suspend fun handleInOrderEvent(event: StockPotEvent): StockState {
        applyEvent(event)
        lastEventTime = event.eventDateTime
        lastRequestId = event.requestId

        // Tell the snapshotter that an event was processed
        snapshotStrategy.onEventProcessed(currentState)

        return currentState
    }

    private suspend fun recreateStateFromEvents(): StockState {
        logger.debug { "Recreating state from events due to out-of-order event" }
        currentState = StockState(currentState.location, currentState.productId)
        val events = repository.getEventsInChronologicalOrder(locationId, productId)
        processEvents(events)
        return currentState
    }

    private suspend fun processEvents(events: Iterable<StockPotEvent>): Int {
        val eventList = events.toList()
        var eventCount = 0
        eventList.forEach { event ->
            applyEvent(event)
            lastEventTime = event.eventDateTime
            eventCount++

            // Tell the snapshotter that an event was processed
            snapshotStrategy.onEventProcessed(currentState)
        }
        // lastRequestId should be the last persisted request, not the chronologically last event
        this.lastRequestId = repository.getLastPersistedRequestId(locationId, productId)
        return eventCount
    }

    private suspend fun persistEvent(event: StockPotEvent) {
        logger.logOperation("persisting event $event") {
            repository.insert(locationId, productId, event)
        }
    }

    private suspend fun applyEvent(stockPotEvent: StockPotEvent): StockState {
        currentState = stockPotEvent.apply(currentState)
        return currentState
    }
}
