package com.darren.stock.domain.actors

import kotlinx.coroutines.CompletableDeferred

abstract class UntrackedStockPotMessages {
    class GetValue(val response: CompletableDeferred<Double>) : UntrackedStockPotMessages() {
        override fun toString(): String {
            return "GetValue(response=$response)"
        }
    }
}