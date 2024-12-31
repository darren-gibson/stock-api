package org.darren.stock.domain.actors.events

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import java.time.LocalDateTime

class DeliveryEvent(
    val quantity: Double,
    val supplierId: String,
    val supplierRef: String,
    val eventTime: LocalDateTime,
    result: CompletableDeferred<Reply>
) : StockPotMessages(result) {
    override suspend fun execute(state: StockState) = state.copy(quantity = state.quantity + quantity, lastUpdated = eventTime)

    override fun toString(): String {
        return "DeliveryEvent(eventTime=$eventTime, quantity=$quantity, supplierId='$supplierId', supplierRef='$supplierRef')"
    }
}