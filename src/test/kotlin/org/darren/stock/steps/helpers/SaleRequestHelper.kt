package org.darren.stock.steps.helpers

import kotlinx.coroutines.runBlocking
import org.darren.stock.steps.ApiCallStepDefinitions
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import kotlin.getValue

internal class SaleRequestHelper : KoinComponent {
    private val apiCallStepDefinitions by inject<ApiCallStepDefinitions>()
    private val dateTimeProvider: TestDateTimeProvider by inject()

    fun performSale(
        locationId: String,
        productId: String,
        quantity: Double,
        requestId: String = UUID.randomUUID().toString(),
    ) = runBlocking {
        val url = "/locations/$locationId/products/$productId/sales"
        val soldAt = dateTimeProvider.nowAsString()
        val payload = """{ "requestId": "$requestId", "soldAt": "$soldAt", "quantity": $quantity }"""

        return@runBlocking apiCallStepDefinitions.sendPostRequest(url, payload)
    }
}
