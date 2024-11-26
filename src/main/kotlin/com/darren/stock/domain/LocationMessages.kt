package com.darren.stock.domain

import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDateTime

sealed class LocationMessages(val locationId: String) {
    class DefineLocationEvent(locationId: String, val eventTime: LocalDateTime, val parentLocationId: String?) : LocationMessages(locationId) {
        override fun toString(): String {
            return "DefineLocationEvent(locationId=$locationId, eventTime=$eventTime, parentLocationId=$parentLocationId)"
        }
    }

    class GetAllChildrenForParentLocation(locationId: String, val deferred: CompletableDeferred<Map<String, String>>) : LocationMessages(locationId) {
        override fun toString(): String {
            return "GetAllChildrenForParentLocation(locationId=$locationId, deferred=$deferred)"
        }
    }
}
