package org.darren.stock.infrastructureTests

import arrow.resilience.Schedule
import arrow.resilience.retry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class ArrowKtTest {
    val retryPolicy =
        Schedule.exponential<Throwable>(100.milliseconds)

    @Test
    fun `schedule can run suspended tasks`() =
        runTest {
            val result =
                retryPolicy.retry {
                    suspendedFunction(2)
                }
            assertEquals(2, result)
        }

    @Test
    fun `retriable tasks can be run in parallel`() =
        runTest {
            val results = mutableListOf<Long>()

            listOf(
                async { runSuspendedFunctionAndPlaceResultInList(50, results) },
                async { runSuspendedFunctionAndPlaceResultInList(0, results) },
            ).awaitAll()

            assertContentEquals(listOf(0L, 50L), results)
        }

    private suspend fun runSuspendedFunctionAndPlaceResultInList(
        delayMillis: Long,
        results: MutableList<Long>,
    ) {
        results.add(suspendedFunction(delayMillis))
    }

    suspend fun suspendedFunction(delayMillis: Long): Long {
        delay(delayMillis)
        return delayMillis
    }
}
