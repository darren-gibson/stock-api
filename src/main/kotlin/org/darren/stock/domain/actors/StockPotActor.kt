package org.darren.stock.domain.actors

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.actor4k.actor.Actor
import io.github.smyrgeorge.actor4k.actor.Behavior
import org.darren.stock.domain.*
import org.darren.stock.domain.actors.StockPotProtocol.GetValue
import org.darren.stock.domain.actors.StockPotProtocol.RecordInternalMoveTo
import org.darren.stock.domain.actors.StockPotProtocol.RecordMove
import org.darren.stock.domain.actors.StockPotProtocol.Reply
import org.darren.stock.domain.actors.StockPotProtocol.StockPotRequest
import org.darren.stock.domain.actors.events.*

class StockPotActor(
    key: String,
    private val repository: StockEventRepository,
) : Actor<StockPotProtocol, Reply>(key) {
    private val productLocation = ProductLocation.parse(key)
    private val locationId = productLocation.locationId
    private val productId = productLocation.productId
    private val idempotencyService = IdempotencyService(repository)
    private val stateManager = StockStateManager(locationId, productId, repository)
    private val logger = KotlinLogging.logger {}
    private val eventFactory = StockPotEventFactory(productId, locationId)

    init {
        logger.debug { "Creating StockPotActor location=$locationId, product=$productId" }
    }

    override suspend fun onShutdown() {
        logger.info { "[$this ${address()}] onShutdown" }
        super.onShutdown()
    }

    override suspend fun onBeforeActivate() {
        logger.info { "[${address()}] onBeforeActivate" }
        stateManager.initializeState()
        super.onBeforeActivate()
    }

    override suspend fun onActivate(m: StockPotProtocol) {
        // Optional override.
        logger.info { "[${address()}] onActivate: $m" }
        super.onActivate(m)
    }

    override suspend fun onReceive(m: StockPotProtocol): Behavior<Reply> {
        logger.debug { "$this, MSG=$m" }
        return when (m) {
            is GetValue -> processEvent(m)
            is RecordInternalMoveTo -> processEvent(m)
            is RecordMove -> handleIdempotentOperation(m) { validateMove(it as RecordMove, stateManager.currentState) }
            is StockPotRequest -> handleIdempotentOperation(m)
        }.also { logger.debug { this } }
    }

    private suspend fun processEvent(message: StockPotProtocol): Behavior<Reply> {
        val event = eventFactory.createEvent(message)
        val newState = stateManager.processEvent(event)
        return Behavior.Reply(Reply(newState))
    }

    private suspend fun handleIdempotentOperation(
        message: StockPotProtocol,
        preProcess: ((StockPotProtocol) -> Unit)? = null,
    ): Behavior<Reply> {
        if (idempotencyService.checkIdempotency(message as StockPotRequest)) {
            return Behavior.Reply(Reply(stateManager.currentState))
        }
        preProcess?.invoke(message)
        return processEvent(message)
    }

    private fun validateMove(
        recordMove: RecordMove,
        currentState: StockState,
    ) {
        if (currentState.quantity!! < recordMove.quantity) {
            throw InsufficientStockException()
        }
    }

    override fun toString(): String = "StockPotActor(state='${stateManager.currentState}')"
}
