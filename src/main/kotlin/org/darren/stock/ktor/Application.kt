package org.darren.stock.ktor

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span.current
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.darren.stock.config.KoinModules
import org.darren.stock.ktor.Delivery.deliveryEndpoint
import org.darren.stock.ktor.GetStock.getStockEndpoint
import org.darren.stock.ktor.Move.moveEndpoint
import org.darren.stock.ktor.Sale.saleEndpoint
import org.darren.stock.ktor.Status.statusEndpoint
import org.darren.stock.ktor.StockCount.stockCountEndpoint
import org.darren.stock.ktor.exception.ExceptionHandlerChain
import org.koin.core.context.startKoin
import org.slf4j.MDC

fun main(args: Array<String>) {
    startKoin {
        modules(KoinModules.allModules())
    }
    io.ktor.server.netty.EngineMain
        .main(args)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
    val logger = KotlinLogging.logger {}
    // Install OpenTelemetry plugin for trace context propagation
    val openTelemetry = getOpenTelemetry("Stock API")
    install(KtorServerTelemetry) {
        setOpenTelemetry(openTelemetry)
    }
    OpenTelemetryAppender.install(openTelemetry)
    logger.info { "OpenTelemetry API API started" }

    interceptAndApplyOtelLoggingContext(logger)

    install(ContentNegotiation) {
        println("DEBUG: Installing ContentNegotiation")
        json(
            Json { decodeEnumsCaseInsensitive = true },
        )
    }
    install(StatusPages) { handleExceptions() }
    routing {
        moveEndpoint()
        statusEndpoint()
        stockCountEndpoint()
        saleEndpoint()
        deliveryEndpoint()
        getStockEndpoint()
    }
}

private fun Application.interceptAndApplyOtelLoggingContext(logger: KLogger) {
    intercept(ApplicationCallPipeline.Setup) {
        // Try to get trace ID from OpenTelemetry span
        val span = current()
        logger.debug { "span.spanContext.isValid = ${span.spanContext.isValid}" }

        var traceId: String? = null
        var spanId: String? = null
        if (span.spanContext.isValid) {
            traceId = span.spanContext.traceId
            spanId = span.spanContext.spanId
            logger.debug { "Trace from OpenTelemetry traceId=$traceId, spanId=$spanId" }
        } else {
            logger.warn { "OpenTelemetry span is not valid" }
        }

        if (traceId != null) {
            MDC.put("trace_id", traceId)
        }
        if (spanId != null) {
            MDC.put("span_id", spanId)
        }

        try {
            withContext(MDCContext()) {
                proceed()
            }
        } finally {
            MDC.remove("trace_id")
            MDC.remove("span_id")
        }
    }
}

private fun StatusPagesConfig.handleExceptions() {
    exception<Throwable> { call, cause ->
        if (!ExceptionHandlerChain.handle(call, cause)) {
            throw cause
        }
    }
}

private var openTelemetrySdk: OpenTelemetry? = null

fun getOpenTelemetry(serviceName: String): OpenTelemetry {
    // Return cached instance if already initialized (may be set by tests)
    openTelemetrySdk?.let { return it }

    val sdk =
        AutoConfiguredOpenTelemetrySdk
            .builder()
            .addPropertiesSupplier { mapOf("otel.service.name" to serviceName) }
            .build()
            .openTelemetrySdk
    // Set as global instance so client instrumentation can access it
    runCatching { GlobalOpenTelemetry.set(sdk) }
    openTelemetrySdk = sdk
    return sdk
}

// Allow tests to provide their own OpenTelemetry instance
fun setOpenTelemetryForTests(openTelemetry: OpenTelemetry) {
    openTelemetrySdk = openTelemetry
    runCatching { GlobalOpenTelemetry.set(openTelemetry) }
}
