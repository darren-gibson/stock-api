package org.darren.stock.steps.helpers

import io.cucumber.java.ParameterType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

@Suppress("unused")
class CucumberHelper {
    @ParameterType("([+-]?([0-9]*[.])?[0-9]+)(?: (?:cans?|box|boxes|bottles?|packets?|kg))")
    fun quantity(value: String): Double = value.toDouble()

    @ParameterType("(\\d\\d:\\d\\d) on (\\d{4}-\\d\\d-\\d\\d)")
    fun dateTime(
        time: String,
        date: String,
    ): LocalDateTime = LocalDateTime.parse("${date}T$time", ISO_LOCAL_DATE_TIME)
}
