package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.prometheus.client.Counter
import net.logstash.logback.argument.StructuredArguments.kv
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

private const val DEFAULT_CHANNEL = "#team-bømlo-alerts"

class Consumer(
    private val config: Config,
    clientId: String = UUID.randomUUID().toString().slice(1..5)
) {
    private val consumer =
        KafkaConsumer(config.consumerConfig(clientId, config.consumerGroup), StringDeserializer(), StringDeserializer())
    private val running = AtomicBoolean(false)
    private val validator = JsonSchemaValidator()

    companion object {
        private val total_counter = Counter.build().labelNames("slack_channel", "failing_app")
            .name("spydig_validation_errors_total").help("Total errors.").register()
    }

    private fun consumeMessages() {
        var lastException: Exception? = null
        try {
            consumer.subscribe(listOf(config.topic))
            while (running.get()) {
                consumer.poll(Duration.ofSeconds(1)).onEach {
                    handleMessages(it.value())
                }
            }
        } catch (err: WakeupException) {
            // throw exception if we have not been told to stop
            if (running.get()) throw err
        } catch (err: Exception) {
            lastException = err
            throw err
        } finally {
            closeResources(lastException)
        }
    }

    fun start() {
        logger.info("starting spydig")
        if (running.getAndSet(true)) return logger.info("spydig already started")

        consumeMessages()
    }

    private val objectMapper = jacksonObjectMapper()

    // team sykmelding er eneste som skiller på alerts fra spydig i dev og prod
    private fun teamTilKanaler(sykmelding: String) : Map<String, String> {
        return mapOf(
            "spleis" to "#team-bømlo-alerts",
            "syfosmregler" to sykmelding,
            "syfosoknad" to "#flex",
            "flex-syketilfelle" to "#flex",
            "syfosmpapirregler" to sykmelding,
            "riskvurderer-sykdom" to "#helse-risk-alerts",
        )
    }

    private fun handleMessages(value: String) {
        sikkerlogger.info("Leser melding med {}", kv("json", value))
        val melding = objectMapper.readTree(value)
        if (melding.path("eventName").asText() != "subsumsjon") {
            logger.info("melding id: {}, eventName: {} blir ikke validert", melding.path("id").asText(), melding.path("eventName").asText())
            return
        }

        validator.errors(melding)?.let {
            val kilde = melding.get("kilde")?.asText() ?: "null"
            val id = melding.get("id")
            logger.warn("fant feil i melding id: {}, kilde: {}, feil: {}", id, kilde, it)
            sikkerlogger.warn("fant feil: {}, melding: {}", it, value)

            if (kilde == "null") {
                total_counter.labels(DEFAULT_CHANNEL, kilde).inc()
                return
            }
            when (System.getenv()["NAIS_CLUSTER_NAME"]) {
                "dev-gcp" -> {
                    val kanal = teamTilKanaler("#sykmelding-alerts-dev").getOrDefault(kilde, DEFAULT_CHANNEL)
                    total_counter.labels(kanal, kilde).inc()
                } else -> {
                    val kanal = teamTilKanaler("#sykmelding-alerts").getOrDefault(kilde, DEFAULT_CHANNEL)
                    total_counter.labels(kanal, kilde).inc()
                }
            }
        }
    }

    private fun closeResources(lastException: Exception?) {
        if (running.getAndSet(false)) {
            logger.warn("stopped consuming messages due to an error: ", lastException)
        } else {
            logger.info("stopped consuming messages after receiving stop signal")
        }
        tryAndLog(consumer::unsubscribe)
        tryAndLog(consumer::close)
    }

    private fun tryAndLog(block: () -> Unit) {
        try {
            block()
        } catch (err: Exception) {
            logger.error(err.message, err)
        }
    }
}
