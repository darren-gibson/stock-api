package org.darren.stock.steps

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import org.darren.stock.ktor.idempotency.DefaultResponseCacher
import org.darren.stock.ktor.idempotency.IdempotencyMetrics
import org.darren.stock.ktor.idempotency.InMemoryIdempotencyStore
import org.darren.stock.ktor.idempotency.OtelResponseCacher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class IdempotencyMetricsSteps {
    private var sdk: OpenTelemetrySdk? = null
    private lateinit var inMemoryReader: InMemoryMetricReader

    private lateinit var cacher: OtelResponseCacher

    @Given("an in-memory OpenTelemetry meter is configured")
    fun setupOtel() {
        inMemoryReader = InMemoryMetricReader.create()
        val meterProvider = SdkMeterProvider.builder().registerMetricReader(inMemoryReader).build()
        // Build a local OpenTelemetrySdk instance but do NOT register it as the global
        // to avoid conflicts with other tests that may register their own global SDK.
        sdk = OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build()
    }

    @Given("an in-memory idempotency store backed ResponseCacher is available")
    fun setupCacher() {
        val store = InMemoryIdempotencyStore(ttlSeconds = 60, maximumSize = 100)
        val defaultCacher = DefaultResponseCacher(store)
        // Use the local SDK's meter instead of the global to keep tests isolated.
        val meter = sdk!!.getMeter("test.meter")
        cacher = OtelResponseCacher(defaultCacher, meter)
    }

    @When("I request a missing cache key")
    fun requestMissing() {
        cacher.get("missing-1")
    }

    @When("I store a key and then request that key")
    fun storeThenGet() {
        cacher.store("k1", 200, "b", "ct", "h")
        cacher.get("k1")
    }

    @Then("the exported metrics should include {string} and {string}")
    fun assertMetrics(
        hitName: String,
        missName: String,
    ) {
        val metrics = inMemoryReader.collectAllMetrics()
        val hasHits = metrics.any { it.name == hitName }
        val hasMisses = metrics.any { it.name == missName }

        assertTrue(hasHits, "Expected metric $hitName to be exported")
        assertTrue(hasMisses, "Expected metric $missName to be exported")
    }

    @Then("each counter should have value {int}")
    fun assertCounterValues(expected: Int) {
        val metrics = inMemoryReader.collectAllMetrics()

        val hits = metrics.find { it.name == IdempotencyMetrics.HITS }
        val misses = metrics.find { it.name == IdempotencyMetrics.MISSES }

        assertTrue(hits != null, "Expected hits metric to be present")
        assertTrue(misses != null, "Expected misses metric to be present")

        val hitsTotal =
            if (hits!!.longSumData.points.isNotEmpty()) {
                hits.longSumData.points.sumOf { it.value }
            } else {
                hits.doubleSumData.points
                    .sumOf { it.value }
                    .toLong()
            }

        val missesTotal =
            if (misses!!.longSumData.points.isNotEmpty()) {
                misses.longSumData.points.sumOf { it.value }
            } else {
                misses.doubleSumData.points
                    .sumOf { it.value }
                    .toLong()
            }

        assertEquals(expected.toLong(), hitsTotal, "Expected hits counter to equal $expected")
        assertEquals(expected.toLong(), missesTotal, "Expected misses counter to equal $expected")
    }
}
