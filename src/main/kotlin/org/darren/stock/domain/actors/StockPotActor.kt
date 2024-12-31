package org.darren.stock.domain.actors

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import org.darren.stock.domain.Location
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.events.StockPotMessages
import org.koin.core.component.KoinComponent

typealias Reply = Result<StockState>

class StockPotActor(locationId: String, productId: String, initialQuantity: Double) :
    KoinComponent {
    private var currentState = StockState(Location(locationId), productId, initialQuantity)

    companion object {

        private val logger = KotlinLogging.logger {}

        @OptIn(ObsoleteCoroutinesApi::class)
        fun CoroutineScope.stockPotActor(locationId: String, productId: String, initialQuantity: Double = 0.0):
                SendChannel<StockPotMessages> = actor {
            logger.debug { "Creating StockPotActor location=$locationId, product=$productId, quantity=$initialQuantity" }

            with(StockPotActor(locationId, productId, initialQuantity)) {
                for (msg in channel) onReceive(msg)
            }
        }
    }

    private suspend fun onReceive(message: StockPotMessages) {
        logger.debug { "$this, MSG=$message" }

        message.result.complete(Reply.runCatching {
            currentState = message.execute(currentState)
            currentState
        })

        logger.debug { this }
    }

    override fun toString(): String {
        return "StockPotActor(state='$currentState')"
    }
}