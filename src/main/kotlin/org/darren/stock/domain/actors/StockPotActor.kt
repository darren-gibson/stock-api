package org.darren.stock.domain.actors

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.runBlocking
import org.darren.stock.domain.Location
import org.darren.stock.domain.StockEventRepository
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.events.NullStockPotEvent
import org.darren.stock.domain.actors.events.StockPotEvent
import org.darren.stock.domain.actors.messages.StockPotMessages
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

typealias Reply = Result<StockState>

class StockPotActor(locationId: String, productId: String) : KoinComponent {
    private var currentState = StockState(Location(locationId), productId)
    private val repository: StockEventRepository by inject()

    init {
        runBlocking { recreateState() }
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        @OptIn(ObsoleteCoroutinesApi::class)
        fun CoroutineScope.stockPotActor(locationId: String, productId: String):
                SendChannel<StockPotMessages> = actor {
            logger.debug { "Creating StockPotActor location=$locationId, product=$productId" }

            with(StockPotActor(locationId, productId)) {
                for (msg in channel) onReceive(msg)
            }
        }
    }

    private suspend fun onReceive(message: StockPotMessages) {
        logger.debug { "$this, MSG=$message" }

        message.result.complete(Reply.runCatching {
            val stockPotEvent = message.validate(currentState)
            if(stockPotEvent !is NullStockPotEvent) {
                persistEvent(stockPotEvent)
                applyOrRecreateStateIfOutOfOrderEvent(stockPotEvent)
            }
            else
                applyEvent(stockPotEvent)
        })

        logger.debug { this }
    }

    private suspend fun StockPotActor.applyOrRecreateStateIfOutOfOrderEvent(stockPotEvent: StockPotEvent) =
        if (stockPotEvent.eventDateTime.isBefore(currentState.lastUpdated)) {
            logger.debug { "${stockPotEvent.eventDateTime} is before current state: ${currentState.lastUpdated}" }
            recreateState()
        }
        else
            applyEvent(stockPotEvent)

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

    override fun toString(): String {
        return "StockPotActor(state='$currentState')"
    }
}