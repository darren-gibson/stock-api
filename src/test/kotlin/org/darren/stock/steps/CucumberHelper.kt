package org.darren.stock.steps

import io.cucumber.java.ParameterType

@Suppress("unused")
class CucumberHelper {
    @ParameterType("([+-]?([0-9]*[.])?[0-9]+)(?: (?:cans?|box|boxes|bottles?|packets?|kg))")
    fun quantity(value: String): Double = value.toDouble()
}