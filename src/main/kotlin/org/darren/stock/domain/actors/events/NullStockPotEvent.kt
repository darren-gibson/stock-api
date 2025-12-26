package org.darren.stock.domain.actors.events

import java.time.LocalDateTime

class NullStockPotEvent : StockPotEvent() {
    override val eventDateTime: LocalDateTime = LocalDateTime.MIN
    override val requestId: String = ""
    override val contentHash: String = ""

    override fun toString(): String = "NullStockPotEvent()"
}
