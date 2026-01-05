package org.darren.stock.ktor.auth

/**
 * Constants for permission components.
 * Permissions follow the format: {resource}:{operation}:{action}
 */
object PermissionConstants {
    /** Permission resources */
    object Resources {
        const val STOCK = "stock"
        const val JOB = "job"
    }

    /** Permission operations */
    object Operations {
        const val COUNT = "count"
        const val LEVEL = "level"
        const val MOVEMENT = "movement"
        const val PERMISSION = "permission"
    }

    /** Permission actions */
    object Actions {
        const val READ = "read"
        const val WRITE = "write"
    }
}
