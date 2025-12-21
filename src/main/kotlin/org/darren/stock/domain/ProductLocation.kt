package org.darren.stock.domain

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

@JvmInline
value class ProductLocation private constructor(
    private val encoded: String,
) {
    val productId: String
        get() = decode(parts.first)

    val locationId: String
        get() = decode(parts.second)

    override fun toString(): String = encoded

    private val parts: Pair<String, String>
        get() {
            val split = encoded.split(SEPARATOR, limit = 2)
            require(split.size == 2) { "Invalid ProductLocation format" }
            return split[0] to split[1]
        }

    companion object {
        private const val SEPARATOR = "|"

        fun of(
            productId: String,
            locationId: String,
        ): ProductLocation {
            require(productId.isNotBlank()) { "productId must not be blank" }
            require(locationId.isNotBlank()) { "locationId must not be blank" }

            val encodedProduct = encode(productId)
            val encodedLocation = encode(locationId)

            return ProductLocation("$encodedProduct$SEPARATOR$encodedLocation")
        }

        fun parse(value: String): ProductLocation = ProductLocation(value)

        private fun encode(value: String): String = URLEncoder.encode(value, UTF_8)

        private fun decode(value: String): String = URLDecoder.decode(value, UTF_8)
    }
}
