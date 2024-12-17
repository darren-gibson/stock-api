package org.darren.stock.domain.actors

import org.darren.stock.domain.LocationType
import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDateTime

sealed class LocationMessages(val locationId: String) {
    class DefineLocationEvent(
        id: String, val type: LocationType, val eventTime: LocalDateTime, val parentId: String?
    ) : LocationMessages(id) {
        override fun toString(): String {
            return "DefineLocationEvent(locationId=$locationId, locationType=$type, eventTime=$eventTime, parentLocationId=$parentId)"
        }
    }

    class GetAllChildrenForParentLocation(locationId: String, val result: CompletableDeferred<Map<String, String>>) :
        LocationMessages(locationId) {
        override fun toString(): String {
            return "GetAllChildrenForParentLocation(locationId=$locationId, result=$result)"
        }
    }

    class GetLocationType(locationId: String, val result: CompletableDeferred<Result<LocationType>>) : LocationMessages(locationId) {
        override fun toString(): String {
            return "GetLocationType(locationId=$locationId, result=$result)"
        }

    }
}
