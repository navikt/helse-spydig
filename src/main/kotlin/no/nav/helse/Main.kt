package no.nav.helse

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Clock.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry.defaultRegistry
import io.prometheus.client.Counter
import io.prometheus.client.exporter.common.TextFormat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

var logger: Logger = LoggerFactory.getLogger("Application")

fun main() {
    val config = Config.fromEnv()
    ConsumerRunner(config, ::ktorServer).startBlocking()
}

fun ktorServer(appName: String, isReady: () -> Boolean): ApplicationEngine =
    embeddedServer(CIO, applicationEngineEnvironment {
        /**
         * Konfigurasjon av Webserver (Ktor https://ktor.io/)
         */

        val appMicrometerRegistry = defaultRegistry

        val total_counter = Counter.build().labelNames("slack_channel")
            .name("spydig_test").help("Total errors.").register()

        log = logger
        connector {
            port = 8080
        }
        module {
            install(ContentNegotiation) { jackson() }
            install(CallLogging) {
                disableDefaultColors()
                filter { call ->
                    call.request.path().startsWith("/hello")
                }
            }
            install(MicrometerMetrics) {
                registry = PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT,
                    appMicrometerRegistry,
                    SYSTEM
                )
            }

            routing {
                get("/") {
                    call.respondText(
                        "<html><h1>$appName</h1><html>",
                        ContentType.Text.Html
                    )
                }

                get("/hello") {
                    total_counter.labels("spydig-test").inc()
                    call.respondText("Hello")
                }

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