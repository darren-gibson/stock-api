import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("io.cucumber:cucumber-java:7.20.1")
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")

    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.20.1")
    testImplementation("org.junit.platform:junit-platform-suite:1.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")

}
tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}