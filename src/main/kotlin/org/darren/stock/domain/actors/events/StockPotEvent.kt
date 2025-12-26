package org.darren.stock.domain.actors.events

import kotlinx.serialization.Serializable
import org.darren.stock.domain.DateTimeProvider
import org.darren.stock.domain.StockState
import org.darren.stock.util.DateSerializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime

@Serializable
abstract class StockPotEvent : KoinComponent {
    abstract val eventDateTime: LocalDateTime
    abstract val requestId: String
    abstract val contentHash: String

    @Serializable(with = DateSerializer::class)
    val recordedDataTime: LocalDateTime = currentDateTime()

    private fun currentDateTime(): LocalDateTime {
        val dateTimeProvider: DateTimeProvider by inject()
        return dateTimeProvider.now()
    }

    open suspend fun apply(state: StockState): StockState = state
}
