package org.darren.stock.steps.helpers

import org.darren.stock.domain.resilience.ApiResilienceConfig
import org.darren.stock.domain.resilience.BackoffConfig
import org.darren.stock.domain.resilience.FailFastConfig
import kotlin.time.Duration

/**
 * Manages per-scenario resilience configuration for testing.
 * Similar to ActorSystemTestConfig, this allows step definitions to override
 * the default Koin-provided configuration on a scenario-by-scenario basis.
 */
object ResilienceConfigManager {
    private var overrideConfig: ApiResilienceConfig? = null
    private val retryDelays = mutableListOf<Duration>()

    /**
     * Set a test-specific override for resilience configuration.
     * This is called from @Given steps to configure resilience behavior for a scenario.
     */
    fun setOverrideConfig(config: ApiResilienceConfig) {
        overrideConfig = config
    }

    /**
     * Get the current override configuration, or null if using the default Koin config.
     */
    fun getOverrideConfig(): ApiResilienceConfig? = overrideConfig

    /**
     * Clear the override configuration (called in @After hooks).
     */
    fun clearOverride() {
        overrideConfig = null
    }

    fun recordRetryDelay(delay: Duration) {
        retryDelays.add(delay)
    }

    fun getRetryDelays(): List<Duration> = retryDelays.toList()

    fun clearRetryDelays() {
        retryDelays.clear()
    }

    /**
     * Build a resilience config from settings parsed from feature file.
     */
    fun buildConfig(settings: Map<String, String>): ApiResilienceConfig {
        val backoff =
            BackoffConfig(
                maxAttempts = settings["resilience.apis.location.backoff.maxAttempts"]?.toIntOrNull() ?: 3,
                initialDelay = Duration.parse(settings["resilience.apis.location.backoff.initialDelay"] ?: "100ms"),
                maxDelay = Duration.parse(settings["resilience.apis.location.backoff.maxDelay"] ?: "2s"),
                multiplier = settings["resilience.apis.location.backoff.multiplier"]?.toDoubleOrNull() ?: 2.0,
            )

        val failFast =
            FailFastConfig(
                failureThreshold = settings["resilience.apis.location.failFast.failureThreshold"]?.toIntOrNull() ?: 5,
                quietPeriod = Duration.parse(settings["resilience.apis.location.failFast.quietPeriod"] ?: "60s"),
                testAfterQuietPeriod = settings["resilience.apis.location.failFast.testAfterQuietPeriod"]?.toBoolean() ?: true,
            )

        return ApiResilienceConfig(backoff = backoff, failFast = failFast)
    }
}
