package org.darren.stock.steps.helpers

import org.darren.stock.domain.DateTimeProvider
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TestDateTimeProvider : DateTimeProvider {
    private var dateTime: LocalDateTime? = null

    override fun now(): LocalDateTime = dateTime ?: LocalDateTime.now()

    fun setDateTime(dateTime: LocalDateTime) {
        this.dateTime = dateTime
    }

    fun nowAsString() = asString(now())

    fun asString(dateTime: LocalDateTime): String = DateTimeFormatter.ISO_DATE_TIME.format(dateTime)
}
