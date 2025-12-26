@file:Suppress("LocalVariableName")

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("io.ktor.plugin") version "3.3.3"
    id("com.diffplug.spotless") version "8.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
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
    implementation("io.ktor:ktor-server-core-jvm:3.3.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.3.3")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.3.3")
    implementation("io.ktor:ktor-server-status-pages:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.3.3")
    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-cio:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    runtimeOnly("io.insert-koin:koin-core:4.2.0-beta2")
    implementation("io.insert-koin:koin-ktor:4.2.0-beta2")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.22")

    // Actor4k for actor system
    implementation("io.github.smyrgeorge:actor4k:1.4.5")

    // JWT for authentication
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.1")
    testImplementation("io.cucumber:cucumber-java:7.33.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.33.0")
    testImplementation("org.junit.platform:junit-platform-suite:6.0.1")
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.3.3")
    testImplementation("io.ktor:ktor-client-mock:3.3.3")
    // Ensure Kotlin reflection is available on the test classpath for dynamic module loading
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:2.2.21")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.21")
    testImplementation("io.insert-koin:koin-test:4.1.1")
    testImplementation("io.insert-koin:koin-test-junit5:4.1.1")

    // Caffeine for in-memory cache with expiry
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")
    // OpenTelemetry API for metrics
    implementation("io.opentelemetry:opentelemetry-api:1.57.0")
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
    jvmToolchain(21)
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

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "22"
}

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

// Dependency-Check is executed in CI (see .GitHub/workflows/security.yml)
