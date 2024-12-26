package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.InsufficientStockException
import org.darren.stock.domain.MoveResult
import org.darren.stock.domain.StockMovement
import org.darren.stock.domain.actors.StockPotMessages
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.actors.StockPotMessages.*
import java.time.LocalDateTime

object Move {
    suspend fun StockSystem.move(movement: StockMovement) {
        val from = getStockPot(movement.from, movement.product)
        val to = getStockPot(movement.to, movement.product)

        move(movement, from, to)
//            throw OperationNotSupportedException("both ${movement.from} and ${movement.to} must be Tracked locations.")
    }

    private suspend fun move(
        move: StockMovement,
        from: SendChannel<StockPotMessages>,
        to: SendChannel<StockPotMessages>
    ) {
        val result = CompletableDeferred<MoveResult>()

        with(move) {
            from.send(MoveEvent(product, quantity, to, reason, LocalDateTime.now(), result))
            when (result.await()) {
                MoveResult.Success -> return
                MoveResult.InsufficientStock -> throw InsufficientStockException()
            }
        }
    }
}