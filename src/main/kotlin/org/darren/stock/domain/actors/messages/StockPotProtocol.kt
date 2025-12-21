package org.darren.stock.domain.actors.messages

import io.github.smyrgeorge.actor4k.actor.ActorProtocol
import io.github.smyrgeorge.actor4k.actor.ref.ActorRef
import java.time.LocalDateTime
import org.darren.stock.domain.MovementReason
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.StockState
import org.darren.stock.domain.actors.StockPotActor

sealed interface StockPotProtocol : ActorProtocol {
        sealed class Message<R : ActorProtocol.Response> :
                StockPotProtocol, ActorProtocol.Message<R>()

        class GetValue() : Message<Reply>()

        data class RecordCount(
                val eventTime: LocalDateTime,
                val quantity: Double,
                val reason: StockCountReason
        ) : Message<Reply>()

        data class RecordDelivery(
                val quantity: Double,
                val supplierId: String,
                val supplierRef: String,
                val eventTime: LocalDateTime
        ) : Message<Reply>()

        data class RecordInternalMoveTo(
                val productId: String,
                val quantity: Double,
                val from: String,
                val reason: MovementReason,
                val eventTime: LocalDateTime
        ) : Message<Reply>()

        data class RecordMove(
                val quantity: Double,
                val to: ActorRef,
                val reason: MovementReason,
                val eventTime: LocalDateTime,
        ) : Message<Reply>()

        data class RecordSale(val eventTime: LocalDateTime, val quantity: Double) :
                Message<Reply>()

        data class Reply(val result: Result<StockState>) : ActorProtocol.Response()
}
