package org.darren.stock.domain.resilience

import kotlin.time.Duration

/**
 * Configuration for backoff (retry) behavior.
 */
data class BackoffConfig(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = Duration.parse("100ms"),
    val maxDelay: Duration = Duration.parse("2s"),
    val multiplier: Double = 2.0,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be at least 1, got $maxAttempts" }
        require(initialDelay >= Duration.ZERO) { "initialDelay must be non-negative, got $initialDelay" }
        require(maxDelay >= initialDelay) { "maxDelay ($maxDelay) must be >= initialDelay ($initialDelay)" }
        require(multiplier >= 1.0) { "multiplier must be >= 1.0, got $multiplier" }
    }
}

/**
 * Configuration for fail-fast behavior.
 */
data class FailFastConfig(
    val failureThreshold: Int = 5,
    val quietPeriod: Duration = Duration.parse("60s"),
    val testAfterQuietPeriod: Boolean = true,
) {
    init {
        require(failureThreshold >= 1) { "failureThreshold must be at least 1, got $failureThreshold" }
        require(quietPeriod > Duration.ZERO) { "quietPeriod must be positive, got $quietPeriod" }
    }
}

/**
 * Complete resilience configuration for an external API.
 */
data class ApiResilienceConfig(
    val backoff: BackoffConfig = BackoffConfig(),
    val failFast: FailFastConfig = FailFastConfig(),
) {
    init {
        require(failFast.failureThreshold >= backoff.maxAttempts) {
            "failureThreshold (${failFast.failureThreshold}) must be >= maxAttempts (${backoff.maxAttempts}) " +
                "to allow retries before circuit opens"
        }
    }
}
