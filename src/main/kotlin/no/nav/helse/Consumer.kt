package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.prometheus.client.Counter
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class Consumer(
    private val config: Config,
    clientId: String = UUID.randomUUID().toString().slice(1..5)
) {
    private val consumer =
        KafkaConsumer(config.consumerConfig(clientId, config.consumerGroup), StringDeserializer(), StringDeserializer())
    private val running = AtomicBoolean(false)
    private val logger = LoggerFactory.getLogger(Consumer::class.java)
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

    private fun handleMessages(value: String) {
        val kildeSpleis = "spleis"
        val kildeSyfosmregler = "syfosmregler"
        val kildeSyfosoknad = "syfosoknad"
        val kildeFlexSyketilfelle = "flex-syketilfelle"
        val kildeSyfosmPapirRegler = "syfosmpapirregler"

        val melding = objectMapper.readTree(value)
        if (!validator.isJSONvalid(melding)) {
            val kilde = melding.get("kilde")
            if (kilde == null) {
                total_counter.labels("#område-helse-etterlevelse", "null").inc()
                logger.info("kilde mangler i melding")
                return
            }
            when (kilde.asText()) {
                kildeSpleis -> {
                    total_counter.labels("#team-bømlo-værsågod", kildeSpleis).inc()
                    logger.info("fant feil hos $kildeSpleis, sender melding")
                }
                kildeSyfosmregler -> {
                    total_counter.labels("#team-sykmelding", kildeSyfosmregler).inc()
                    logger.info("fant feil hos $kildeSyfosmregler, sender melding")
                }
                kildeSyfosoknad -> {
                    total_counter.labels("#flex", kildeSyfosoknad).inc()
                    logger.info("fant feil hos $kildeSyfosoknad, sender melding")
                }
                kildeFlexSyketilfelle -> {
                    total_counter.labels("#flex", kildeFlexSyketilfelle).inc()
                    logger.info("fant feil hos $kildeFlexSyketilfelle, sender melding")
                }
                kildeSyfosmPapirRegler -> {
                    total_counter.labels("#team-sykmelding", kildeSyfosmPapirRegler).inc()
                    logger.info("fant feil hos $kildeSyfosmPapirRegler, sender melding")
                }
                else -> {
                    total_counter.labels("#område-helse-etterlevelse", kilde.asText()).inc()
                    logger.info("fant feil i schema hvor kilde ikke gjenkjennes. Denne kilden er ${kilde.asText()}")
                }
            }
        }
    }

    private fun closeResources(lastException: Exception?) {
        if (running.getAndSet(false)) {
            logger.warn("stopped consuming messages due to an error", lastException)
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