
val kafkaVersion = "3.6.1"
val ktorVersion = "3.0.1"
val jsonSchemaValidatorVersion = "1.2.0"
val junitJupiterVersion = "5.11.3"

plugins {
    kotlin("jvm") version "2.0.21"
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
    implementation("org.slf4j:slf4j-api:2.0.11")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")

    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.networknt:json-schema-validator:$jsonSchemaValidatorVersion")


    api("io.micrometer:micrometer-registry-prometheus:1.12.3")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("21"))
    }
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("skipped", "failed")
        }
    }

    jar {
        archiveFileName.set("app.jar")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.MainKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }
        doLast {
            configurations.runtimeClasspath.get()
                .filter { it.name != "app.jar" }
                .forEach {
                    val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                    if (!file.exists()) it.copyTo(file)
                }
        }
    }
}