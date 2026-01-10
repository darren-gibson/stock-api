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
import org.darren.stock.domain.snapshot.SnapshotStrategyFactory

/**
 * Actor managing stock state for a single product-location pair.
 *
 * ## Thread Safety
 *
 * This actor is thread-safe by design. Actor4k ensures that messages to this actor are processed
 * sequentially in a single-threaded manner. Multiple actors can process messages concurrently
 * without interference due to complete state isolation.
 *
 * ## Lifecycle
 *
 * - **Creation**: Created lazily via `ActorSystem.get()` on first access
 * - **Activation**: `onBeforeActivate()` rehydrates state from event store and snapshots
 * - **Message Processing**: `onReceive()` handles messages sequentially in arrival order
 * - **Shutdown**: `onShutdown()` called during eviction or system shutdown
 * - **Eviction**: Automatically removed from registry after configured inactivity period
 * - **Rehydration**: On next access, state is replayed from event store
 *
 * ## Message Processing
 *
 * All state changes are idempotent and event-sourced. Messages with duplicate `requestId`
 * are detected via the idempotency service and return the current state without modification.
 *
 * @see StockSystem
 * @see StockStateManager
 */
class StockPotActor(
    key: String,
    repository: StockEventRepository,
    snapshotStrategyFactory: SnapshotStrategyFactory,
) : Actor<StockPotProtocol, Reply>(key) {
    private val productLocation = ProductLocation.parse(key)
    private val locationId = productLocation.locationId
    private val productId = productLocation.productId
    private val idempotencyService = IdempotencyService(repository)
    private val stateManager = StockStateManager(locationId, productId, repository, snapshotStrategyFactory)
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
            is GetValue -> Behavior.Reply(Reply(stateManager.currentState))
            is RecordInternalMoveTo -> processEvent(m)
            is RecordMove -> handleIdempotentOperation(m) { validateMove(it as RecordMove, stateManager.currentState) }
            is StockPotRequest -> handleIdempotentOperation(m, preProcess = { validateRequest(it as StockPotRequest) })
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
        // Safe: quantity is guaranteed non-null after first event (Delivery, Count, or Override)
        if (currentState.quantity!! < recordMove.quantity) {
            throw InsufficientStockException()
        }
    }

    private fun validateRequest(request: StockPotRequest) {
        when (request) {
            is StockPotProtocol.RecordDelivery -> {
                require(request.quantity > 0) { "Delivery quantity must be positive" }
            }
            // Add other validations as needed
        }
    }

    override fun toString(): String = "StockPotActor(state='${stateManager.currentState}')"
}
