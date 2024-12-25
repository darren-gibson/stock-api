package org.darren.stock.infrastructureTests

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.darren.stock.domain.LocationRoles
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class JsonSerializerTest {
    @Serializable
    data class TestClass(val l: LocationRoles? = null)

    @Test
    fun `test Unknown Enum Can Be Deserialized`() {
        val coercingJson = Json {
            coerceInputValues = true
        }

        val test = coercingJson.decodeFromString<TestClass>("""{"l": "unknown"}""")
        assertNull(test.l)
    }
}