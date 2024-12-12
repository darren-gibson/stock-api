import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    application
}

group = "com.darren.stock"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val ktor_version = "2.3.13"
//    val kotlinVersion = "2.1.0"
//    val koin_version = "4.0.0"
    val koin_version = "3.5.6"

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("io.cucumber:cucumber-java:7.20.1")
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation(project.dependencies.platform("io.insert-koin:koin-bom:$koin_version"))
    implementation("io.insert-koin:koin-core")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktor_version")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.20.1")
    testImplementation("org.junit.platform:junit-platform-suite:1.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("net.javacrumbs.json-unit:json-unit:4.1.0")
    testImplementation("org.hamcrest:hamcrest:3.0")

    implementation("com.github.cukedoctor:cukedoctor-maven-plugin:3.9.0")
    implementation("com.github.cukedoctor:cukedoctor-section-layout:3.9.0")
}
tasks.test {
    useJUnitPlatform()
}

/** Copies files from "build/distributions" to "demo" directory */
tasks.register<Exec>("cukedoctor") {
    commandLine("java", "-cp", "src/test/resources/cukedoctor-section-layout-3.9.0.jar:src/test/resources/cukedoctor-main-3.9.0.jar:.",
        "com.github.cukedoctor.CukedoctorMain", "-hideScenarioKeyword", "-t", "Simple Stock API", "-toc", "left")
   project
}

tasks.named("test") { finalizedBy("cukedoctor") }

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}