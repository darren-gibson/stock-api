package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.SendChannel
import org.darren.stock.domain.StockMovement
import org.darren.stock.domain.actors.MoveEvent
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.StockPotMessages
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
        val result = CompletableDeferred<Reply>()

        with(move) {
            from.send(MoveEvent(product, quantity, to, reason, LocalDateTime.now(), result))
            result.await().getOrThrow()
        }
    }
}