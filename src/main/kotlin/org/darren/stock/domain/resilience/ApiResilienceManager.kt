package org.darren.stock.domain.resilience

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lightweight holder for the resilience configuration. Previously handled retry logic itself; now it
 * broadcasts config changes so HTTP clients can rebuild Arrow resilience plugins when tests override
 * the settings.
 */
open class ApiResilienceManager(
    initialConfig: ApiResilienceConfig,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @Volatile
    private var config: ApiResilienceConfig = initialConfig

    private val listeners = CopyOnWriteArrayList<(ApiResilienceConfig) -> Unit>()

    fun currentConfig(): ApiResilienceConfig = config

    fun updateConfig(newConfig: ApiResilienceConfig) {
        config = newConfig
        logger.debug { "Updated resilience config: $newConfig" }
        listeners.forEach { it(newConfig) }
    }

    fun reset() {
        listeners.forEach { it(config) }
    }

    fun onConfigChanged(listener: (ApiResilienceConfig) -> Unit) {
        listeners.add(listener)
    }
}
