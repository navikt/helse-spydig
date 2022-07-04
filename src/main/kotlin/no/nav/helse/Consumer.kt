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

    internal fun isRunning() = running.get()
    private fun consumeMessages() {
        var lastException: Exception? = null
        try {
            consumer.subscribe(listOf(config.topic))
            while (running.get()) {
                consumer.poll(Duration.ofSeconds(1)).also { records ->
                    records.forEach {
                        handleMessages(it.value())
                    }
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

    fun stop() {
        logger.info("stopping spydig")
        if (!running.getAndSet(false)) return logger.info("spydig already stopped")
        consumer.wakeup()
    }

    private val objectMapper = jacksonObjectMapper()

    private fun handleMessages(value: String) {
        val slackKanal = "#spydig-test"
        // TODO: Endre denne til å mappe til rett slack gruppe for prod

        val kildeSpleis = "spleis"
        val kildeSyfosmregler = "syfosmregler"
        val kildeSyfosoknad = "syfosoknad"
        val kildeFlexSyketilfelle = "flex-syketilfelle"
        val kildeSyfosmPapirRegler = "syfosmpapirregler"

        val melding = objectMapper.readTree(value)
        if (!validator.isJSONvalid(melding)) {
            val kilde = melding.get("kilde")
            if (kilde == null) {
                total_counter.labels(slackKanal, "null kilde").inc()
                logger.info("kilde mangler i melding")
                return
            }
            when (kilde.asText()) {
                kildeSpleis -> {
                    total_counter.labels(slackKanal, kildeSpleis).inc()
                    logger.info("fant feil hos $kildeSpleis, sender melding")
                }
                kildeSyfosmregler -> {
                    total_counter.labels(slackKanal, kildeSyfosmregler).inc()
                    logger.info("fant feil hos $kildeSyfosmregler, sender melding")
                }
                kildeSyfosoknad -> {
                    total_counter.labels(slackKanal, kildeSyfosoknad).inc()
                    logger.info("fant feil hos $kildeSyfosoknad, sender melding")
                }
                kildeFlexSyketilfelle -> {
                    total_counter.labels(slackKanal, kildeFlexSyketilfelle).inc()
                    logger.info("fant feil hos $kildeFlexSyketilfelle, sender melding")
                }
                kildeSyfosmPapirRegler -> {
                    total_counter.labels(slackKanal, kildeSyfosmPapirRegler).inc()
                    logger.info("fant feil hos $kildeSyfosmPapirRegler, sender melding")
                }
                else -> {
                    total_counter.labels(slackKanal, "Ukjent kilde").inc()
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