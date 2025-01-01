package org.darren.stock.steps

import io.cucumber.java.en.Given
import org.darren.stock.steps.helpers.TestDateTimeProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime

class DateTimeHelperStepDefinitions: KoinComponent {
    private val dateTimeProvider: TestDateTimeProvider by inject()

    @Given("it's {dateTime}")
    fun itIs(dateTime: LocalDateTime) {
        dateTimeProvider.setDateTime(dateTime)
    }
}