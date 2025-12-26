package org.darren.stock.domain.actors

import io.github.oshai.kotlinlogging.KotlinLogging
import org.darren.stock.domain.*
import org.darren.stock.domain.actors.events.StockPotEvent
import java.time.LocalDateTime

class StockStateManager(
    private val locationId: String,
    private val productId: String,
    private val repository: StockEventRepository,
) {
    private val logger = KotlinLogging.logger {}
    var currentState = StockState(Location(locationId), productId)
        private set
    var lastEventTime: LocalDateTime? = null
        private set

    suspend fun initializeState() {
        recreateStateFromEvents()
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
        return currentState
    }

    private suspend fun recreateStateFromEvents(): StockState {
        logger.debug { "Recreating state from events due to out-of-order event" }
        currentState = StockState(currentState.location, currentState.productId)
        val events = repository.getEvents(locationId, productId)
        currentState = events.fold(currentState) { state, event -> event.apply(state) }

        // Update lastEventTime to the latest event time
        lastEventTime = events.maxOfOrNull { it.eventDateTime }

        return currentState
    }

    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> logOperation(
        operation: String,
        block: () -> T,
    ): T {
        logger.debug { "Starting $operation" }
        return try {
            block().also { logger.debug { "Completed $operation" } }
        } catch (e: Throwable) {
            logger.error(e) { "Failed $operation" }
            throw e
        }
    }

    private suspend fun persistEvent(event: StockPotEvent) {
        logOperation("persisting event $event") {
            repository.insert(locationId, productId, event)
        }
    }

    private suspend fun applyEvent(stockPotEvent: StockPotEvent): StockState {
        currentState = stockPotEvent.apply(currentState)
        return currentState
    }
}
