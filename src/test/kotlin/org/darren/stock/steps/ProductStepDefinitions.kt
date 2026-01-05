package org.darren.stock.steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Given

class ProductStepDefinitions {
    @And("a valid product code {string} exists")
    @Given("{string} is a product")
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun aValidProductCodeExists(productId: String) {
        // Placeholder: Future implementation will set up product data
    }
}
