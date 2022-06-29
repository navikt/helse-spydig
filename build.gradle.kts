import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kafkaVersion = "3.1.0"

plugins {
    kotlin("jvm") version "1.6.21"
    application
}

group = "no.nav.helse"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.slf4j:slf4j-api:1.7.36")

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