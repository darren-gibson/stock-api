package org.darren.stock.domain.actors

import org.darren.stock.domain.Location
import org.darren.stock.domain.actors.LocationMessages.*
import org.darren.stock.domain.LocationNotFoundException
import org.darren.stock.domain.LocationType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.lang.NullPointerException

class LocationActor {
    companion object {
        private val logger = KotlinLogging.logger {}

        @OptIn(ObsoleteCoroutinesApi::class)
        fun CoroutineScope.locationActor(): SendChannel<LocationMessages> = actor {
            with(LocationActor()) {
                for (msg in channel) onReceive(msg)
            }
        }
    }

    private val locationMap = mutableMapOf<String, Location>()

    private fun onReceive(msg: LocationMessages) {
        logger.debug { "message received: $msg" }
        when (msg) {
            is DefineLocationEvent -> defineLocation(msg.locationId, msg.type, msg.parentId)
            is GetAllChildrenForParentLocation -> getAllChildrenForParentLocation(msg.locationId, msg.result)
            is GetLocationType -> {
                msg.result.complete(
                    try {
                        Result.success(locationMap[msg.locationId]!!.type)
                    } catch (e: NullPointerException) {
                        Result.failure(LocationNotFoundException(msg.locationId))
                    }
                )
            }
        }
    }

    private fun getAllChildrenForParentLocation(parent: String, result: CompletableDeferred<Map<String, String>>) {
        val descendantsMap = mutableMapOf<String, String>()

        fun findDescendants(currentParent: String) {
            val children = locationMap.filterValues { it.parent == currentParent }.keys
            for (child in children) {
                descendantsMap[child] = currentParent
                findDescendants(child)
            }
        }

        findDescendants(parent)
        result.complete(descendantsMap)
    }

    private fun defineLocation(id: String, type: LocationType, parentId: String?) {
        locationMap[id] = Location(id, type, parentId)
    }
}