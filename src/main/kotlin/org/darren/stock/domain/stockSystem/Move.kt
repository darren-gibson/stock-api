package org.darren.stock.domain.stockSystem

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.MovementReason
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.messages.RecordMove
import java.time.LocalDateTime

object Move {
    suspend fun StockSystem.move(
        from: String, to: String, product: String, quantity: Double, reason: MovementReason, movedAt: LocalDateTime
    ) {
        locations.ensureLocationsAreTracked(from, to)
        val fromPot = getStockPot(from, product)
        val toPot = getStockPot(to, product)
        val result = CompletableDeferred<Reply>()

        fromPot.send(RecordMove(quantity, toPot, reason, movedAt, result))
        result.await().getOrThrow()
    }
}