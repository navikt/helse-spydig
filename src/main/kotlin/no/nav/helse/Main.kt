package no.nav.helse

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Clock.SYSTEM
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

var logger: Logger = LoggerFactory.getLogger("Spydig")
var sikkerlogger: Logger = LoggerFactory.getLogger("tjenestekall")

fun main() {
    val config = Config.fromEnv()
    ConsumerRunner(config, ::ktorServer).startBlocking()
}

fun ktorServer(meterRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, SYSTEM)) =
    embeddedServer(CIO, applicationEnvironment {
        log = logger
    }, configure = {
        connector {
            port = 8080
        }
    }) {
        install(ContentNegotiation) { jackson() }
        install(MicrometerMetrics) {
            registry = meterRegistry
        }

        routing {
            get("/metrics") {
                call.respond(meterRegistry.scrape())
            }

            get("/isalive") {
                call.respondText("OK")
            }

            get("/isready") {
                call.respondText("OK")
            }
        }
    }