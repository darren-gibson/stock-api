package org.darren.stock.domain.actors.events

import java.time.LocalDateTime

class NullStockPotEvent :  StockPotEvent() {
    override val eventDateTime: LocalDateTime = LocalDateTime.MIN

    override fun toString(): String {
        return "NullStockPotEvent()"
    }
}