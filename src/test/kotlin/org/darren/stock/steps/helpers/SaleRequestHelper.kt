package org.darren.stock.steps.helpers

import io.ktor.client.statement.*
import org.darren.stock.steps.ApiCallStepDefinitions
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import kotlin.getValue

internal class SaleRequestHelper : KoinComponent {
    private val apiCallStepDefinitions by inject<ApiCallStepDefinitions>()
    private val dateTimeProvider: TestDateTimeProvider by inject()

    suspend fun performSale(
        locationId: String,
        productId: String,
        quantity: Double,
        requestId: String = UUID.randomUUID().toString(),
    ): HttpResponse {
        val url = "/locations/$locationId/products/$productId/sales"
        val soldAt = dateTimeProvider.nowAsString()
        val payload = """{ "requestId": "$requestId", "soldAt": "$soldAt", "quantity": $quantity }"""

        return apiCallStepDefinitions.sendPostRequest(url, payload)
    }
}
