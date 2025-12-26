package org.darren.stock.domain.stockSystem

import org.darren.stock.domain.actors.StockPotProtocol

object Move {
    suspend fun StockSystem.recordMovement(command: MoveCommand) {
        with(command) {
            locations.ensureLocationsAreTracked(fromLocationId, toLocationId)
            val toPot = getStockPot(toLocationId, productId)

            val recordMove = StockPotProtocol.RecordMove(quantity, toPot, reason, movedAt, requestId)
            val contentHash = recordMove.contentHash()

            val fromPot = getStockPot(fromLocationId, productId)

            // TODO: Make Duration a configurable timeout
            val result = fromPot.ask(recordMove)
            result.getOrThrow().result
        }
    }
}
