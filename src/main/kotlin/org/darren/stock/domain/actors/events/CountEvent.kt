package org.darren.stock.domain.actors.events

import kotlinx.serialization.Serializable
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.StockState
import org.darren.stock.util.DateSerializer
import java.time.LocalDateTime

@Serializable
class CountEvent(
    @Serializable(with = DateSerializer::class)
    override val eventDateTime: LocalDateTime,
    val quantity: Double,
    val reason: StockCountReason,
    override val requestId: String,
    override val contentHash: String,
) : StockPotEvent() {
    override suspend fun apply(state: StockState) = state.copy(quantity = quantity, lastUpdated = eventDateTime, lastRequestId = requestId)

    override fun toString(): String = "CountEvent(eventTime=$eventDateTime, quantity=$quantity, reason=$reason, requestId='$requestId', contentHash='$contentHash')"
}
