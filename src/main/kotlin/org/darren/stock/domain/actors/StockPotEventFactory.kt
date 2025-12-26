package org.darren.stock.domain.actors

import org.darren.stock.domain.*
import org.darren.stock.domain.actors.StockPotProtocol.*
import org.darren.stock.domain.actors.events.*

class StockPotEventFactory(
    private val productId: String,
    private val locationId: String,
) {
    suspend fun createEvent(
        protocol: StockPotProtocol,
    ): StockPotEvent =
        when (protocol) {
            is GetValue -> NullStockPotEvent()
            is RecordCount -> createCountEvent(protocol)
            is RecordDelivery -> createDeliveryEvent(protocol)
            is RecordInternalMoveTo -> createInternalMoveToEvent(protocol)
            is RecordMove -> createMoveEvent(protocol)
            is RecordSale -> createSaleEvent(protocol)
            else -> throw IllegalArgumentException("Unsupported protocol: $protocol")
        }

    private fun createCountEvent(protocol: RecordCount): CountEvent =
        CountEvent(
            protocol.eventTime,
            protocol.quantity,
            protocol.reason,
            protocol.requestId,
            protocol.contentHash(),
        )

    private fun createDeliveryEvent(protocol: RecordDelivery): DeliveryEvent =
        DeliveryEvent(
            protocol.quantity,
            protocol.supplierId,
            protocol.supplierRef,
            protocol.requestId,
            protocol.contentHash(),
            protocol.eventTime,
        )

    private fun createInternalMoveToEvent(protocol: RecordInternalMoveTo): InternalMoveToEvent =
        InternalMoveToEvent(
            productId,
            protocol.quantity,
            protocol.from,
            protocol.reason,
            protocol.eventTime,
        )

    private suspend fun createMoveEvent(
        protocol: RecordMove,
    ): MoveEvent {
        val destinationState = performInterActorMove(protocol)
        return MoveEvent(
            protocol.quantity,
            destinationState.location.id,
            protocol.reason,
            protocol.eventTime,
            protocol.requestId,
            protocol.contentHash(),
        )
    }

    private fun createSaleEvent(protocol: RecordSale): SaleEvent =
        SaleEvent(
            protocol.eventTime,
            protocol.quantity,
            protocol.requestId,
            protocol.contentHash(),
        )

    private suspend fun performInterActorMove(recordMove: RecordMove): StockState =
        recordMove.to
            .ask(
                RecordInternalMoveTo(
                    productId,
                    recordMove.quantity,
                    locationId,
                    recordMove.reason,
                    recordMove.eventTime,
                ),
            ).getOrThrow()
            .result
}
