package org.darren.stock.ktor.exception

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import org.darren.stock.ktor.InvalidValuesException

/**
 * Generic recursive field extractor utility that eliminates duplication
 * in extracting specific types of nested exception fields.
 */
object RecursiveFieldExtractor {
    /**
     * Recursively extracts missing fields from MissingFieldException.
     *
     * @param cause The exception to search through
     * @return List of missing field names if found, null otherwise
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun extractMissingFields(cause: Throwable): List<String>? {
        if (cause is MissingFieldException) return cause.missingFields
        return cause.cause?.let { extractMissingFields(it) }
    }

    /**
     * Recursively extracts invalid values from InvalidValuesException.
     *
     * @param cause The exception to search through
     * @return List of invalid values if found, null otherwise
     */
    fun extractInvalidValues(cause: Throwable): List<String>? {
        if (cause is InvalidValuesException) return cause.fields
        return cause.cause?.let { extractInvalidValues(it) }
    }
}
