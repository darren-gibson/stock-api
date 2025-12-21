package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.messages.StockPotProtocol
import org.darren.stock.domain.actors.messages.StockPotProtocol.RecordMove
import kotlin.time.Duration.Companion.seconds

object Move {
    suspend fun StockSystem.recordMovement(command: MoveCommand) {
        with(command) {
            locations.ensureLocationsAreTracked(fromLocationId, toLocationId)
            val fromPot = getStockPot(fromLocationId, productId)
            val toPot = getStockPot(toLocationId, productId)

            // TODO: Make Duration a configurable timeout
            fromPot.ask(StockPotProtocol.RecordMove(quantity, toPot, reason, movedAt), 5.seconds)
        }
    }
}
