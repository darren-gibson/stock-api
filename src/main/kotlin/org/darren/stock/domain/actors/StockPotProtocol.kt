package org.darren.stock.domain.actors

import io.github.smyrgeorge.actor4k.actor.ActorProtocol
import io.github.smyrgeorge.actor4k.actor.ref.ActorRef
import org.darren.stock.domain.MovementReason
import org.darren.stock.domain.StockCountReason
import org.darren.stock.domain.StockState
import java.security.MessageDigest
import java.time.LocalDateTime

sealed interface StockPotProtocol : ActorProtocol {
    sealed class Message<R : ActorProtocol.Response> :
        ActorProtocol.Message<R>(),
        StockPotProtocol

    abstract class StockPotRequest(
        open val requestId: String,
    ) : Message<Reply>() {
        abstract fun contentHash(): String
    }

    class GetValue : Message<Reply>()

    data class RecordCount(
        val eventTime: LocalDateTime,
        val quantity: Double,
        val reason: StockCountReason,
        override val requestId: String,
    ) : StockPotRequest(requestId) {
        override fun contentHash(): String = hashFields(eventTime, quantity, reason)
    }

    data class RecordDelivery(
        val quantity: Double,
        val supplierId: String,
        val supplierRef: String,
        val eventTime: LocalDateTime,
        override val requestId: String,
    ) : StockPotRequest(requestId) {
        override fun contentHash(): String = hashFields(quantity, supplierId, supplierRef, eventTime)
    }

    data class RecordInternalMoveTo(
        val productId: String,
        val quantity: Double,
        val from: String,
        val reason: MovementReason,
        val eventTime: LocalDateTime,
    ) : Message<Reply>() {
        fun contentHash(): String = hashFields(productId, quantity, from, reason, eventTime)
    }

    data class RecordMove(
        val quantity: Double,
        val to: ActorRef,
        val reason: MovementReason,
        val eventTime: LocalDateTime,
        override val requestId: String,
    ) : StockPotRequest(requestId) {
        override fun contentHash(): String = hashFields(quantity, to, reason, eventTime)
    }

    data class RecordSale(
        val eventTime: LocalDateTime,
        val quantity: Double,
        override val requestId: String,
    ) : StockPotRequest(requestId) {
        override fun contentHash(): String = hashFields(eventTime, quantity)
    }

    data class Reply(
        val result: StockState,
    ) : ActorProtocol.Response()

    companion object {
        fun hashFields(vararg fields: Any): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val content = fields.joinToString("|") { it.toString() }
            val hashBytes = digest.digest(content.toByteArray())
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}
