package com.darren.stock.domain

import com.darren.stock.domain.handlers.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.component.KoinComponent
import java.time.LocalDateTime
import org.koin.core.component.inject

class StockSystem(private val helper: HandlerHelper) : KoinComponent {
    private val saleHandler by inject<SaleHandler>()
    private val countHandler by inject<CountHandler>()
    private val deliveryHandler by inject<DeliveryHandler>()
    private val getValueHandler by inject<GetValueHandler>()
    private val moveHandler by inject<MoveHandler>()

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    suspend fun setInitialStockLevel(locationId: String, productId: String, initialQuantity: Double) {
        helper.stockPots[locationId to productId] =
            helper.createInitialChannelType(locationId, productId, initialQuantity)
    }

    suspend fun sale(locationId: String, productId: String, quantity: Double, eventTime: LocalDateTime) {
        saleHandler.sale(locationId, productId, quantity, eventTime)
    }

    suspend fun count(locationId: String, productId: String, quantity: Double, reason: StockCountReason, time: LocalDateTime) {
        countHandler.count(locationId, productId, quantity, reason, time)
    }

    suspend fun delivery(locationId: String, productId: String, quantity: Double, time: LocalDateTime) {
        deliveryHandler.delivery(locationId, productId, quantity, time)
    }

    suspend fun getValue(locationId: String, productId: String): Double {
        return getValueHandler.getValue(locationId, productId)
    }

    suspend fun move(movement: StockMovement) {
        moveHandler.move(movement)
    }
}