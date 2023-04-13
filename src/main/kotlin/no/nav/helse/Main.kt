package no.nav.helse

import io.ktor.http.ContentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Clock.SYSTEM
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry.defaultRegistry
import io.prometheus.client.exporter.common.TextFormat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

var logger: Logger = LoggerFactory.getLogger("Spydig")
var sikkerlogger: Logger = LoggerFactory.getLogger("tjenestekall")

fun main() {
    val config = Config.fromEnv()
    ConsumerRunner(config, ::ktorServer).startBlocking()
}

fun ktorServer(): ApplicationEngine =
    embeddedServer(CIO, applicationEngineEnvironment {
        val appMicrometerRegistry = defaultRegistry

        log = logger
        connector {
            port = 8080
        }
        module {
            install(ContentNegotiation) { jackson() }
            install(MicrometerMetrics) {
                registry = PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT,
                    appMicrometerRegistry,
                    SYSTEM
                )
            }

            routing {
                get("/metrics") {
                    call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                        TextFormat.write004(this, appMicrometerRegistry.metricFamilySamples())
                    }
                }

                get("/isalive") {
                    call.respondText("OK")
                }

                get("/isready") {
                    call.respondText("OK")
                }
            }
        }
    })