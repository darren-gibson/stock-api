package org.darren.stock.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.darren.stock.ktor.InvalidValuesException
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DateUtilsTest {
    @Serializable
    data class Wrapper(
        @Serializable(with = DateSerializer::class) val ts: LocalDateTime,
    )

    private val json = Json { }

    @Test
    fun `serialize and deserialize LocalDateTime`() {
        val now = LocalDateTime.of(2025, 12, 15, 12, 0, 0)
        val wrapper = Wrapper(now)
        val encoded = json.encodeToString(Wrapper.serializer(), wrapper)
        val decoded = json.decodeFromString(Wrapper.serializer(), encoded)
        assertEquals(wrapper.ts, decoded.ts)
    }

    @Test
    fun `invalid date throws InvalidValuesException`() {
        val bad = "{\"ts\":\"not-a-date\"}"
        assertFailsWith<InvalidValuesException> {
            json.decodeFromString(Wrapper.serializer(), bad)
        }
    }
}
