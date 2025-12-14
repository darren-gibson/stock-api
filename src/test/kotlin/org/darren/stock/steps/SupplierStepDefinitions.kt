package org.darren.stock.steps

import io.cucumber.java.en.Given

class SupplierStepDefinitions {
    @Given("{string} is a registered supplier")
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun isARegisteredSupplier(supplierId: String) {
        // Placeholder: Future implementation will set up supplier data
    }

    @Given("{string} is not a registered supplier")
    @Suppress("UnusedParameter", "UNUSED_PARAMETER")
    fun isNotARegisteredSupplier(supplierId: String) {
        // Placeholder: Future implementation will ensure supplier doesn't exist
    }
}
