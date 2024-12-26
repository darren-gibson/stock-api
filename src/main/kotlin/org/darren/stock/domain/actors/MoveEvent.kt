package org.darren.stock.domain.actors

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.*
import java.time.LocalDateTime

class MoveEvent(
    val productId: String,
    val quantity: Double,
    val to: SendChannel<StockPotMessages>,
    val reason: StockMovementReason,
    val eventTime: LocalDateTime,
    result: CompletableDeferred<Reply>
) : StockPotMessages(result) {
    override suspend fun execute(location: Location, productId: String, currentStock: Double): Double {
        val response = Result.runCatching {
            performMove(location, productId, currentStock)
        }
        result.complete(response)
        return response.getOrElse { currentStock }
    }

    private suspend fun performMove(location: Location, productId: String, currentStock: Double): Double {
        if (currentStock < quantity)
            throw InsufficientStockException()

        val internalMoveResult = CompletableDeferred<Reply>()

        to.send(InternalMoveToEvent(productId, quantity, location.id, reason, eventTime, internalMoveResult))
        internalMoveResult.await().getOrThrow()
        return currentStock - quantity
    }

    override fun toString(): String {
        return "MoveEvent(productId='$productId', quantity=$quantity, to=$to, reason=$reason, eventTime=$eventTime)"
    }
}