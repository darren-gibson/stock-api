@file:Suppress("LocalVariableName")

plugins {
    val ktorVersion = "3.3.3"
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("io.ktor.plugin") version ktorVersion
    id("com.diffplug.spotless") version "8.1.0"
    // Temporarily disabled due to Kotlin incompatibility with Java 25
    // id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("com.github.spotbugs") version "6.4.8"
    id("com.github.ben-manes.versions") version "0.53.0"
}

// Note: OWASP Dependency-Check will be run from CI via the official GitHub Action

group = "org.darren"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "3.3.3"
    runtimeOnly("io.ktor:ktor-server-config-yaml-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // OpenTelemetry integration
    implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-3.0:2.23.0-alpha")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.57.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.57.0")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.23.0-alpha")
    implementation("io.opentelemetry:opentelemetry-exporter-logging:1.57.0") // console exporter
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    runtimeOnly("io.insert-koin:koin-core:4.2.0-beta2")
    implementation("io.insert-koin:koin-ktor:4.2.0-beta2")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.23")

    // Actor4k for actor system
    implementation("io.github.smyrgeorge:actor4k:1.4.6")

    // Arrow KT for functional programming utilities
    implementation("io.arrow-kt:arrow-core-jvm:2.2.1.1")
    implementation("io.arrow-kt:arrow-resilience-jvm:2.2.1.1")

    // JWT for authentication
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.1")
    testImplementation("io.cucumber:cucumber-java:7.33.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.33.0")
    testImplementation("org.junit.platform:junit-platform-suite:6.0.1")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    // Ensure Kotlin reflection is available on the test classpath for dynamic module loading
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:2.3.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.3.0")
    testImplementation("io.insert-koin:koin-test:4.1.1")
    testImplementation("io.insert-koin:koin-test-junit5:4.1.1")

    // OpenTelemetry API for metrics
    implementation("io.opentelemetry:opentelemetry-api:1.57.0")
    // MDC propagation for coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.10.1")
    // SDK and testing artifacts used in integration tests
    testImplementation("io.opentelemetry:opentelemetry-sdk:1.57.0")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.57.0")
    testImplementation("net.javacrumbs.json-unit:json-unit:5.1.0")
    testImplementation("org.hamcrest:hamcrest:3.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    // SpotBugs FindSecBugs plugin for security-focused bug patterns
    // Note: spotbugsPlugins configuration is used by the SpotBugs Gradle plugin
    // See: https://spotbugs.github.io/ and https://find-sec-bugs.github.io/
    // The actual declaration below uses the plugin configuration at the end of the file.
}

tasks.test {
    useJUnitPlatform()
    // Disable Ktor's development auto-reload during tests to avoid dynamic module loading
    systemProperty("io.ktor.development", "false")
}

// Generates living documentation from Cucumber test results
tasks.register<Exec>("cukedoctor") {
    commandLine(
        "java",
        "-cp",
        "src/test/resources/cukedoctor-section-layout-3.9.0.jar:src/test/resources/cukedoctor-main-3.9.0.jar:.",
        "com.github.cukedoctor.CukedoctorMain",
        "-hideScenarioKeyword",
        "-t",
        "Stock API",
        "-toc",
        "left",
        "-o",
        "build/docs/stock-api",
        "-p",
        "build/test-results",
    )
}

// Only run cukedoctor for production builds, not during development/testing
// Run explicitly with: ./gradlew cukedoctor
// Or as part of: ./gradlew build (but not ./gradlew test)
tasks.named("assemble") { finalizedBy("cukedoctor") }

application {
    mainClass = "org.darren.stock.ktor.ApplicationKt"
}

kotlin {
    jvmToolchain(25)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("build/**/*.kt")
        ktlint("1.8.0")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.8.0")
    }
}

// Temporarily disabled due to Kotlin incompatibility with Java 25
// detekt {
//     buildUponDefaultConfig = true
//     allRules = false
//     config.setFrom("$projectDir/detekt.yml")
// }

// Temporarily disabled due to Kotlin incompatibility with Java 25
// tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
//     jvmTarget = "21"
// }

// Configure SpotBugs
spotbugs {
    toolVersion.set("4.9.8")
    // Do not fail the build locally on SpotBugs findings — CI will surface issues.
    ignoreFailures.set(true)
}

// Add FindSecBugs plugin for SpotBugs
dependencies {
    // Find security-focused bug patterns
    add("spotbugsPlugins", "com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0")
}

project(":").setEnvironmentVariablesForOpenTelemetry()

fun Project.setEnvironmentVariablesForOpenTelemetry() {
    tasks.withType<JavaExec> {
        environment("OTEL_METRICS_EXPORTER", "none")
        environment("OTEL_TRACES_EXPORTER", "logging")
        environment("OTEL_PROPAGATORS", "tracecontext,baggage")
    }
}

// Dependency-Check is executed in CI (see .GitHub/workflows/security.yml)
