package org.darren.stock.domain.resilience

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ResilienceConfigTest {
    @Test
    fun `BackoffConfig validates maxAttempts is at least 1`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                BackoffConfig(maxAttempts = 0)
            }
        assertEquals("maxAttempts must be at least 1, got 0", exception.message)
    }

    @Test
    fun `BackoffConfig validates initialDelay is non-negative`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                BackoffConfig(initialDelay = (-1).milliseconds)
            }
        assertEquals("initialDelay must be non-negative, got -1ms", exception.message)
    }

    @Test
    fun `BackoffConfig allows zero initialDelay for immediate retry`() {
        BackoffConfig(initialDelay = 0.milliseconds)
        // Should not throw - zero delay is valid for immediate retry
    }

    @Test
    fun `BackoffConfig validates maxDelay is greater than or equal to initialDelay`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                BackoffConfig(initialDelay = 200.milliseconds, maxDelay = 100.milliseconds)
            }
        assertEquals("maxDelay (100ms) must be >= initialDelay (200ms)", exception.message)
    }

    @Test
    fun `BackoffConfig validates multiplier is at least 1_0`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                BackoffConfig(multiplier = 0.5)
            }
        assertEquals("multiplier must be >= 1.0, got 0.5", exception.message)
    }

    @Test
    fun `BackoffConfig allows valid configuration`() {
        BackoffConfig(
            maxAttempts = 3,
            initialDelay = 100.milliseconds,
            maxDelay = 2.seconds,
            multiplier = 2.0,
        )
        // Should not throw
    }

    @Test
    fun `FailFastConfig validates failureThreshold is at least 1`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                FailFastConfig(failureThreshold = 0)
            }
        assertEquals("failureThreshold must be at least 1, got 0", exception.message)
    }

    @Test
    fun `FailFastConfig validates quietPeriod is positive`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                FailFastConfig(quietPeriod = 0.seconds)
            }
        assertEquals("quietPeriod must be positive, got 0s", exception.message)
    }

    @Test
    fun `FailFastConfig allows valid configuration`() {
        FailFastConfig(
            failureThreshold = 5,
            quietPeriod = 60.seconds,
            testAfterQuietPeriod = true,
        )
        // Should not throw
    }

    @Test
    fun `ApiResilienceConfig validates failureThreshold is greater than or equal to maxAttempts`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                ApiResilienceConfig(
                    backoff = BackoffConfig(maxAttempts = 5),
                    failFast = FailFastConfig(failureThreshold = 3),
                )
            }
        assertEquals(
            "failureThreshold (3) must be >= maxAttempts (5) to allow retries before circuit opens",
            exception.message,
        )
    }

    @Test
    fun `ApiResilienceConfig allows failureThreshold equal to maxAttempts`() {
        ApiResilienceConfig(
            backoff = BackoffConfig(maxAttempts = 3),
            failFast = FailFastConfig(failureThreshold = 3),
        )
        // Should not throw
    }

    @Test
    fun `ApiResilienceConfig allows failureThreshold greater than maxAttempts`() {
        ApiResilienceConfig(
            backoff = BackoffConfig(maxAttempts = 3),
            failFast = FailFastConfig(failureThreshold = 5),
        )
        // Should not throw
    }

    @Test
    fun `ApiResilienceConfig allows default configuration`() {
        ApiResilienceConfig()
        // Should not throw - defaults are valid
    }
}
