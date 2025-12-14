package org.darren.stock.domain.service

import kotlinx.serialization.Serializable
import org.darren.stock.domain.StockLevel
import org.darren.stock.ktor.DateSerializer
import java.time.LocalDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Application service that encapsulates the domain interactions required to
 * produce the stock response used by the HTTP endpoint.
 *
 * Extracting this logic into a service makes the endpoint thinner and the
 * mapping easier to unit test.
 */
class StockService(
    private val stockReader: StockReader,
    private val locationValidator: org.darren.stock.domain.service.LocationValidator,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun getStockResponse(
        locationId: String,
        productId: String,
        includeChildren: Boolean,
    ): GetStockResponseDTO {
        logger.debug { "StockService.getStockResponse called: locationId=$locationId productId=$productId includeChildren=$includeChildren" }
        locationValidator.ensureValidLocation(locationId)

        val stockLevel: StockLevel = stockReader.retrieveValue(locationId, productId, includeChildren)

        val dto = mapToDto(stockLevel, includeChildren)
        try {
            val json = Json.encodeToString(dto)
            logger.debug { "StockService.getStockResponse result JSON=$json" }
        } catch (e: Exception) {
            logger.debug(e) { "Failed to serialize DTO for logging" }
        }

        return dto
    }

    private fun mapToDto(
        stockLevel: StockLevel,
        includeChildren: Boolean,
    ): GetStockResponseDTO =
        if (includeChildren) {
            GetStockResponseDTO(
                locationId = stockLevel.locationId,
                productId = stockLevel.state.productId,
                quantity = stockLevel.quantity,
                pendingAdjustment = stockLevel.pendingAdjustment,
                lastUpdated = stockLevel.lastUpdated,
                totalQuantity = stockLevel.totalQuantity,
                childLocations = stockLevel.childLocations.map { mapChild(it) },
            )
        } else {
            GetStockResponseDTO(
                locationId = stockLevel.locationId,
                productId = stockLevel.state.productId,
                quantity = stockLevel.quantity,
                pendingAdjustment = stockLevel.pendingAdjustment,
                lastUpdated = stockLevel.lastUpdated,
            )
        }

    private fun mapChild(stockLevel: StockLevel): ChildLocationsDTO =
        ChildLocationsDTO(
            locationId = stockLevel.locationId,
            quantity = stockLevel.quantity,
            pendingAdjustment = stockLevel.pendingAdjustment,
            totalQuantity = stockLevel.totalQuantity,
            childLocations = stockLevel.childLocations.map { mapChild(it) },
        )

    @Serializable
    data class GetStockResponseDTO(
        val locationId: String,
        val productId: String,
        val quantity: Double? = null,
        val pendingAdjustment: Double = 0.0,
        @Serializable(with = DateSerializer::class)
        val lastUpdated: LocalDateTime,
        val totalQuantity: Double? = null,
        val childLocations: List<ChildLocationsDTO> = emptyList(),
    )

    @Serializable
    data class ChildLocationsDTO(
        val locationId: String,
        val quantity: Double? = null,
        val pendingAdjustment: Double = 0.0,
        val totalQuantity: Double?,
        val childLocations: List<ChildLocationsDTO> = emptyList(),
    )
}
