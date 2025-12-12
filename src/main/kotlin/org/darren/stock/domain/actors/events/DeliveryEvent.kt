package org.darren.stock.domain.actors.events

import kotlinx.serialization.Serializable
import org.darren.stock.domain.StockState
import org.darren.stock.ktor.DateSerializer
import java.time.LocalDateTime

@Serializable
class DeliveryEvent(
    val quantity: Double,
    val supplierId: String,
    val supplierRef: String,
    @Serializable(with = DateSerializer::class)
    override val eventDateTime: LocalDateTime,
) : StockPotEvent() {
    override suspend fun apply(state: StockState) = state.copy(quantity = state.quantity!! + quantity, lastUpdated = eventDateTime)

    override fun toString(): String = "DeliveryEvent(eventTime=$eventDateTime, quantity=$quantity, supplierId='$supplierId', supplierRef='$supplierRef')"
}
