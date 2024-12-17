package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.InsufficientStockException
import org.darren.stock.domain.MoveResult
import org.darren.stock.domain.OperationNotSupportedException
import org.darren.stock.domain.StockMovement
import org.darren.stock.domain.actors.TrackedStockPotMessages
import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDateTime

object Move {
    suspend fun StockSystem.move(movement: StockMovement) {
        val from = getStockPot(movement.from, movement.product)
        val to = getStockPot(movement.to, movement.product)

        if (from is ChannelType.TrackedChannel && to is ChannelType.TrackedChannel) {
            move(movement, from, to)
        } else {
            throw OperationNotSupportedException("both ${movement.from} and ${movement.to} must be Tracked locations.")
        }
    }

    private suspend fun move(move: StockMovement, from: ChannelType.TrackedChannel, to: ChannelType.TrackedChannel) {
        val result = CompletableDeferred<MoveResult>()

        with(move) {
            from.channel.send(TrackedStockPotMessages.MoveEvent(product, quantity, to.channel, reason, LocalDateTime.now(), result))
            when (result.await()) {
                MoveResult.Success -> return
                MoveResult.InsufficientStock -> throw InsufficientStockException()
            }
        }
    }
}