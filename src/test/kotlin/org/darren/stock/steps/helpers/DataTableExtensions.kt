package org.darren.stock.steps.helpers

/**
 * Safely retrieves a required string value from a DataTable row.
 * Throws IllegalArgumentException with a descriptive message if the column is missing or null.
 *
 * @param columnName The name of the column to retrieve
 * @return The non-null string value from the column
 * @throws IllegalArgumentException if the column is missing or contains null
 */
fun Map<String?, String?>.getRequiredString(columnName: String): String =
    this[columnName]
        ?: throw IllegalArgumentException(
            "Required column '$columnName' is missing or null in test data table. " +
                "Available columns: ${this.keys.filterNotNull().joinToString(", ")}",
        )

/**
 * Safely retrieves a required double value from a DataTable row.
 * Throws IllegalArgumentException with a descriptive message if the column is missing, null, or not a valid number.
 *
 * @param columnName The name of the column to retrieve
 * @return The parsed double value from the column
 * @throws IllegalArgumentException if the column is missing, null, or not a valid double
 */
fun Map<String?, String?>.getRequiredDouble(columnName: String): Double {
    val value = getRequiredString(columnName)
    return value.toDoubleOrNull()
        ?: throw IllegalArgumentException(
            "Column '$columnName' contains invalid number: '$value'. Expected a valid double value.",
        )
}

/**
 * Safely retrieves an optional string value from a DataTable row.
 * Returns null if the column is missing or contains null.
 *
 * @param columnName The name of the column to retrieve
 * @return The string value from the column, or null if missing
 */
fun Map<String?, String?>.getOptionalString(columnName: String): String? = this[columnName]
