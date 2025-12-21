package org.darren.stock.domain.actors

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.actor4k.actor.Actor
import io.github.smyrgeorge.actor4k.actor.Behavior
import org.darren.stock.domain.*
import org.darren.stock.domain.actors.events.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime

class StockPotActor(
    key: String,
) : Actor<StockPotProtocol, StockPotProtocol.Reply>(key),
    KoinComponent {
    private val productLocation = ProductLocation.parse(key)
    private val locationId = productLocation.locationId
    private val productId = productLocation.productId
    private val repository: StockEventRepository by inject()
    private var currentState = StockState(Location(locationId), productId)
    private var lastEventTime: LocalDateTime? = null
    private val logger = KotlinLogging.logger {}

    init {
        logger.debug { "Creating StockPotActor location=$locationId, product=$productId" }
    }

    override suspend fun onShutdown() {
        logger.info { "[$this ${address()}] onShutdown" }
        super.onShutdown()
    }

    override suspend fun onBeforeActivate() {
        logger.info { "[${address()}] onBeforeActivate" }
        recreateStateFromEvents()
        super.onBeforeActivate()
    }

    override suspend fun onActivate(m: StockPotProtocol) {
        // Optional override.
        logger.info { "[${address()}] onActivate: $m" }
        super.onActivate(m)
    }

    private suspend fun convertProtocolToEvent(protocol: StockPotProtocol): StockPotEvent =
        when (protocol) {
            is StockPotProtocol.GetValue -> NullStockPotEvent()
            is StockPotProtocol.RecordCount -> CountEvent(protocol.eventTime, protocol.quantity, protocol.reason)
            is StockPotProtocol.RecordDelivery ->
                DeliveryEvent(protocol.quantity, protocol.supplierId, protocol.supplierRef, protocol.eventTime)

            is StockPotProtocol.RecordInternalMoveTo ->
                InternalMoveToEvent(protocol.productId, protocol.quantity, protocol.from, protocol.reason, protocol.eventTime)

            is StockPotProtocol.RecordMove -> handleMove(protocol)
            is StockPotProtocol.RecordSale -> SaleEvent(protocol.eventTime, protocol.quantity)
        }

    private suspend fun handleMove(recordMove: StockPotProtocol.RecordMove): StockPotEvent {
        validateMove(recordMove)
        val destinationState = performInterActorMove(recordMove)
        return createMoveEvent(recordMove, destinationState)
    }

    private fun validateMove(recordMove: StockPotProtocol.RecordMove) {
        if (currentState.quantity!! < recordMove.quantity) {
            throw InsufficientStockException()
        }
    }

    private suspend fun performInterActorMove(recordMove: StockPotProtocol.RecordMove): StockState =
        recordMove.to
            .ask(
                StockPotProtocol.RecordInternalMoveTo(
                    productId,
                    recordMove.quantity,
                    locationId,
                    recordMove.reason,
                    recordMove.eventTime,
                ),
            ).getOrThrow()
            .result

    private fun createMoveEvent(
        recordMove: StockPotProtocol.RecordMove,
        destinationState: StockState,
    ): StockPotEvent =
        MoveEvent(
            recordMove.quantity,
            destinationState.location.id,
            recordMove.reason,
            recordMove.eventTime,
        )

    override suspend fun onReceive(m: StockPotProtocol): Behavior<StockPotProtocol.Reply> {
        logger.debug { "$this, MSG=$m" }
        val event = convertProtocolToEvent(m)
        val newState = processEvent(event)
        return Behavior
            .Reply(StockPotProtocol.Reply(newState))
            .also { logger.debug { this } }
    }

    private suspend fun processEvent(event: StockPotEvent): StockState {
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

    override fun toString(): String = "StockPotActor(state='$currentState')"
}
