package org.darren.stock.domain.actors

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.actor4k.actor.Actor
import io.github.smyrgeorge.actor4k.actor.Behavior
import kotlinx.coroutines.runBlocking
import org.darren.stock.domain.*
import org.darren.stock.domain.actors.events.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StockPotActor(
    key: String,
) : Actor<StockPotProtocol, StockPotProtocol.Reply>(key),
    KoinComponent {
    private val productLocation = ProductLocation.parse(key)
    private val locationId = productLocation.locationId
    private val productId = productLocation.productId
    private val repository: StockEventRepository by inject()
    private var currentState = StockState(Location(locationId), productId)
    private val logger = KotlinLogging.logger {}

    init {
        logger.debug { "Creating StockPotActor location=$locationId, product=$productId" }
    }

    override suspend fun onShutdown() {
        logger.info { "[$this ${address()}] onShutdown" }
        super.onShutdown()
    }

    override suspend fun onBeforeActivate() {
        // Optional override.
        logger.info { "[${address()}] onBeforeActivate" }
        recreateState()
        super.onBeforeActivate()
    }

    override suspend fun onActivate(m: StockPotProtocol) {
        // Optional override.
        logger.info { "[${address()}] onActivate: $m" }
        super.onActivate(m)
    }

    private fun protocolToEvent(m: StockPotProtocol): StockPotEvent =
        when (m) {
            is StockPotProtocol.GetValue -> NullStockPotEvent()
            is StockPotProtocol.RecordCount -> CountEvent(m.eventTime, m.quantity, m.reason)
            is StockPotProtocol.RecordDelivery ->
                DeliveryEvent(m.quantity, m.supplierId, m.supplierRef, m.eventTime)

            is StockPotProtocol.RecordInternalMoveTo ->
                InternalMoveToEvent(m.productId, m.quantity, m.from, m.reason, m.eventTime)

            is StockPotProtocol.RecordMove -> performMove(m)
            is StockPotProtocol.RecordSale -> SaleEvent(m.eventTime, m.quantity)
        }

    private fun performMove(m: StockPotProtocol.RecordMove): StockPotEvent {
        with(currentState) {
            val toState = performMove(productId, location.id, m)
            return MoveEvent(
                m.quantity,
                toState.location.id,
                m.reason,
                m.eventTime,
            )
        }
    }

    private fun performMove(
        productId: String,
        locationId: String,
        m: StockPotProtocol.RecordMove,
    ): StockState {
        if (currentState.quantity!! < m.quantity) {
            throw InsufficientStockException()
        }

        return runBlocking {
            m.to.ask(
                StockPotProtocol.RecordInternalMoveTo(
                    productId,
                    m.quantity,
                    locationId,
                    m.reason,
                    m.eventTime,
                ),
            )
        }.getOrThrow()
            .result
    }

    override suspend fun onReceive(m: StockPotProtocol): Behavior<StockPotProtocol.Reply> {
        logger.debug { "$this, MSG=$m" }

        return Behavior
            .Reply(
                StockPotProtocol.Reply(
                    processEvent(protocolToEvent(m)),
                ),
            ).also {
                logger.debug { this }
            }
    }

    private suspend fun processEvent(stockPotEvent: StockPotEvent): StockState {
        if (stockPotEvent !is NullStockPotEvent) {
            persistEvent(stockPotEvent)
            return applyOrRecreateStateIfOutOfOrderEvent(stockPotEvent)
        } else {
            return applyEvent(stockPotEvent)
        }
    }

    private suspend fun StockPotActor.applyOrRecreateStateIfOutOfOrderEvent(
        stockPotEvent: StockPotEvent,
    ) = if (stockPotEvent.eventDateTime.isBefore(currentState.lastUpdated)) {
        logger.debug {
            "${stockPotEvent.eventDateTime} is before current state: ${currentState.lastUpdated}"
        }
        recreateState()
    } else {
        applyEvent(stockPotEvent)
    }

    private suspend fun recreateState(): StockState {
        currentState = StockState(currentState.location, currentState.productId)
        val events = repository.getEvents(currentState.location.id, currentState.productId)
        currentState = events.fold(currentState) { acc, stockPotEvent -> stockPotEvent.apply(acc) }
        return currentState
    }

    private fun persistEvent(stockPotEvent: StockPotEvent) {
        repository.insert(currentState.location.id, currentState.productId, stockPotEvent)
    }

    private suspend fun applyEvent(stockPotEvent: StockPotEvent): StockState {
        currentState = stockPotEvent.apply(currentState)
        return currentState
    }

    override fun toString(): String = "StockPotActor(state='$currentState')"
}
