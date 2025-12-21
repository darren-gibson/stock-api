package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.actors.StockPotProtocol

object Move {
    suspend fun StockSystem.recordMovement(command: MoveCommand) {
        with(command) {
            locations.ensureLocationsAreTracked(fromLocationId, toLocationId)
            val fromPot = getStockPot(fromLocationId, productId)
            val toPot = getStockPot(toLocationId, productId)

            // TODO: Make Duration a configurable timeout
            val result = fromPot.ask(StockPotProtocol.RecordMove(quantity, toPot, reason, movedAt))
            result.getOrThrow().result
        }
    }
}
