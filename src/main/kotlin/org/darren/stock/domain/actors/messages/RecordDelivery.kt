package org.darren.stock.domain.actors.messages

import kotlinx.coroutines.CompletableDeferred
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.Reply
import org.darren.stock.domain.actors.events.DeliveryEvent
import java.time.LocalDateTime

class RecordDelivery(
    val quantity: Double,
    val supplierId: String,
    val supplierRef: String,
    private val eventTime: LocalDateTime,
    result: CompletableDeferred<Reply>,
) : StockPotMessages(result) {
    override suspend fun validate(state: StockState) = DeliveryEvent(quantity, supplierId, supplierRef, eventTime)

    override fun toString(): String = "RecordDelivery(eventTime=$eventTime, quantity=$quantity, supplierId='$supplierId', supplierRef='$supplierRef')"
}
