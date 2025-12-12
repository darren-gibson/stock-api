@file:Suppress("LocalVariableName")

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("io.ktor.plugin") version "3.3.3"
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

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("io.insert-koin:koin-ktor:4.1.1")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.11")
    implementation("ch.qos.logback:logback-classic:1.5.16")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("io.cucumber:cucumber-java:7.21.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.21.0")
    testImplementation("org.junit.platform:junit-platform-suite:1.11.4")
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.3.3")
    testImplementation("io.ktor:ktor-client-mock:3.3.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.0")
    testImplementation("io.insert-koin:koin-test:4.1.1")
    testImplementation("io.insert-koin:koin-test-junit5:4.1.1")
    testImplementation("net.javacrumbs.json-unit:json-unit:4.1.0")
    testImplementation("org.hamcrest:hamcrest:3.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
}


tasks.test {
    useJUnitPlatform()
}

/** Copies files from "build/distributions" to "demo" directory */
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
        "build/docs/stock-api"
    )
    project
}

tasks.named("test") { finalizedBy("cukedoctor") }

application {
    mainClass = "org.darren.stock.ktor.ApplicationKt"
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}