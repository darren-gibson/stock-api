package org.darren.stock.domain.actors

import org.darren.stock.domain.MoveResult
import org.darren.stock.domain.actors.StockPotMessages.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import org.darren.stock.domain.Location
import org.darren.stock.domain.OperationNotSupportedException
import org.koin.core.component.KoinComponent


class StockPotActor(locationId: String, private val productId: String, initialQuantity: Double) :
    KoinComponent {
    private var currentStock = initialQuantity
    private val location = Location(locationId)

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
        when (message) {
            is GetValue -> message.response.complete(currentStock)
            is SaleEvent -> message.result.complete(performSale(message))
            is DeliveryEvent -> currentStock += message.quantity
            is MoveEvent -> message.result.complete(performMove(message))
            is InternalMoveToEvent -> currentStock += message.quantity
            is CountEvent -> currentStock = message.quantity
        }
        logger.debug { this }
    }

    private suspend fun performSale(saleEvent: SaleEvent): Reply {
        if (location.isShop()) {
            currentStock -= saleEvent.quantity
            return Reply.success(currentStock)
        }
        return Reply.failure(OperationNotSupportedException("Location '${location.id}' is not a shop"))
    }

    private suspend fun performMove(moveEvent: MoveEvent): MoveResult {
        with(moveEvent) {
            if (currentStock < quantity)
                return MoveResult.InsufficientStock

            to.send(InternalMoveToEvent(productId, quantity, location.id, reason, eventTime))
            currentStock -= quantity
        }
        return MoveResult.Success
    }

    override fun toString(): String {
        return "StockPotActor(location='${location.id}', productId='$productId', currentStock=$currentStock)"
    }
}