package org.darren.stock.steps

import io.cucumber.java.en.Given

class SupplierStepDefinitions {
    @Given("{string} is a registered supplier")
    fun isARegisteredSupplier(supplierId: String) {
    }

    @Given("{string} is not a registered supplier")
    fun isNotARegisteredSupplier(supplierId: String) {
    }
}
