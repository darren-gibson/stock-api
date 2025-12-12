package org.darren.stock.ktor

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = LocalDateTime::class)
object DateSerializer : KSerializer<LocalDateTime> {
    private val logger = KotlinLogging.logger {}
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
        } catch (e: DateTimeParseException) {
            val path = decoder.collectDebugPath()
            throw InvalidValuesException(listOf(path), e)
        }
    }

    @Suppress("UNCHECKED_CAST", "SwallowedException")
    private fun Decoder.collectDebugPath(): String {
        try {
            val reader = (this::class as KClass<Any>).memberProperties.first { it.name == "lexer" }.getter(this)!!
            return (reader::class as KClass<Any>)
                .memberProperties
                .first { it.name == "path" }
                .getter(reader)
                .toString()
                .removePrefix("$.")
        } catch (e: NoSuchElementException) {
            logger.debug { "Could not extract field path from decoder, using fallback" }
        } catch (e: IllegalArgumentException) {
            logger.debug { "Could not extract field path from decoder, using fallback" }
        }
        return "Unknown"
    }
}
