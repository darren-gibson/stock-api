package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.messages.RecordMove

object Move {
    suspend fun StockSystem.recordMovement(command: MoveCommand) {
        with(command) {
            locations.ensureLocationsAreTracked(fromLocationId, toLocationId)
            val fromPot = getStockPot(fromLocationId, productId)
            val toPot = getStockPot(toLocationId, productId)
            val result = CompletableDeferred<Reply>()

            fromPot.send(RecordMove(quantity, toPot, reason, movedAt, result))
            result.await().getOrThrow()
        }
    }
}
