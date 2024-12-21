package org.darren.stock

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.darren.stock.domain.LocationClassifications
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class JsonSerializerTest {
    @Serializable
    data class test(val l: LocationClassifications? = null)

    @Test
    fun `test Unknown Enum Can Be Deserialized`() {
        val coercingJson = Json {
            coerceInputValues = true
        }

        val test = coercingJson.decodeFromString<test>("""{"l": "unknown"}""")
        assertNull(test.l)
        // coercingJson.decodeFromString<LocationClassifications?>(""""Unknown"""")
//        coercingJson.decodeFromString<Set<LocationClassifications>>("""["Shop", "Unknown"]""")
    }
}