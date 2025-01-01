package org.darren.stock.domain

import java.time.LocalDateTime

interface DateTimeProvider {
    fun now(): LocalDateTime
}