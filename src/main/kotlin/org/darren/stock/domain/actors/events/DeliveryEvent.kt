package org.darren.stock.domain.actors.events

import kotlinx.serialization.Serializable
import org.darren.stock.domain.StockState
import org.darren.stock.util.DateSerializer
import java.time.LocalDateTime

@Serializable
class DeliveryEvent(
    val quantity: Double,
    val supplierId: String,
    val supplierRef: String,
    override val requestId: String,
    override val contentHash: String,
    @Serializable(with = DateSerializer::class)
    override val eventDateTime: LocalDateTime,
) : StockPotEvent() {
    // Safe: quantity is guaranteed non-null - either initialized to 0.0 or set by prior event
    override suspend fun apply(state: StockState) = state.copy(quantity = state.quantity!! + quantity, lastUpdated = eventDateTime, lastRequestId = requestId)

    override fun toString(): String = "DeliveryEvent(eventTime=$eventDateTime, quantity=$quantity, supplierId='$supplierId', supplierRef='$supplierRef', requestId='$requestId', contentHash='$contentHash')"
}
