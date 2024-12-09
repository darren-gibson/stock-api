package com.darren.stock.domain.stockSystem

import com.darren.stock.domain.actors.TrackedStockPotMessages
import com.darren.stock.domain.actors.UntrackedStockPotMessages
import kotlinx.coroutines.channels.SendChannel

sealed class ChannelType {
    class TrackedChannel(val channel: SendChannel<TrackedStockPotMessages>) : ChannelType()
    class UntrackedChannel(val channel: SendChannel<UntrackedStockPotMessages>) : ChannelType()
}