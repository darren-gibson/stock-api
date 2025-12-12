package org.darren.stock.ktor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = LocalDateTime::class)
object DateSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    override fun serialize(
        encoder: Encoder,
        value: LocalDateTime,
    ) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        try {
            return LocalDateTime.parse(decoder.decodeString(), formatter)
        } catch (e: Exception) {
            val path = decoder.collectDebugPath()
            throw InvalidValuesException(listOf(path), e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Decoder.collectDebugPath(): String {
        try {
            val reader = (this::class as KClass<Any>).memberProperties.first { it.name == "lexer" }.getter(this)!!
            return (reader::class as KClass<Any>)
                .memberProperties
                .first { it.name == "path" }
                .getter(reader)
                .toString()
                .removePrefix("$.")
        } catch (e: Exception) {
            return "Unknown"
        }
    }
}
