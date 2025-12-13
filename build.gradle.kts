@file:Suppress("LocalVariableName")

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("io.ktor.plugin") version "3.3.3"
    id("com.diffplug.spotless") version "7.0.0.BETA4"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
}

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

    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-ktor:4.1.1")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.22")

    // JWT for authentication
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("io.cucumber:cucumber-java:7.33.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.33.0")
    testImplementation("org.junit.platform:junit-platform-suite:1.11.4")
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.3.3")
    testImplementation("io.ktor:ktor-client-mock:3.3.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.21")
    testImplementation("io.insert-koin:koin-test:4.1.1")
    testImplementation("io.insert-koin:koin-test-junit5:4.1.1")
    testImplementation("net.javacrumbs.json-unit:json-unit:4.1.0")
    testImplementation("org.hamcrest:hamcrest:3.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
}

tasks.test {
    useJUnitPlatform()
}

/** Generates living documentation from Cucumber test results */
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

tasks.named("test") { finalizedBy("cukedoctor") }

application {
    mainClass = "org.darren.stock.ktor.ApplicationKt"
}

kotlin {
    jvmToolchain(23)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("build/**/*.kt")
        ktlint("1.5.0").editorConfigOverride(
            mapOf(
                "ktlint_code_style" to "ktlint_official",
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ktlint_standard_filename" to "disabled",
                "ktlint_standard_max-line-length" to "disabled",
            ),
        )
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.5.0")
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
}
