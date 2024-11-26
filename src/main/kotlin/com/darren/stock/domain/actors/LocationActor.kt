package com.darren.stock.domain.actors

import com.darren.stock.domain.LocationMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor

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

    private val locationMap = mutableMapOf<String, String?>()

    fun onReceive(message: LocationMessages) {
        logger.debug { "message received: $message" }
        when (message) {
            is LocationMessages.DefineLocationEvent -> defineLocation(message.locationId, message.parentLocationId)
            is LocationMessages.GetAllChildrenForParentLocation -> getAllChildrenForParentLocation(
                message.locationId,
                message.deferred
            )
        }
    }

    private fun getAllChildrenForParentLocation(
        parentLocation: String,
        deferred: CompletableDeferred<Map<String, String>>
    ) {
        val descendantsMap = mutableMapOf<String, String>()

        fun findDescendants(currentParent: String) {
            val children = locationMap.filterValues { it == currentParent }.keys
            for (child in children) {
                descendantsMap[child] = currentParent
                findDescendants(child)
            }
        }

        findDescendants(parentLocation)
        deferred.complete(descendantsMap)
    }

    private fun defineLocation(locationId: String, parentLocationId: String?) {
        locationMap[locationId] = parentLocationId
    }
}