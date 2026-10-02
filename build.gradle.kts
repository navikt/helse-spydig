group = "no.nav.helse"

plugins {
    alias(libs.plugins.sykepenger.deployable)
}

sykepengerDeployable {
    mainClass = "no.nav.helse.MainKt"
}

dependencies {
    implementation(libs.kafka.clients)
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)

    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.client.cio)
    implementation(libs.json.schema.validator)

    api(libs.micrometer.registry.prometheus.simpleclient)
}
