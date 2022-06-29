package no.nav.helse

import org.slf4j.LoggerFactory
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class Consumer(
    private val config: Config,
    private val etterlevelseTopic: String = "omrade-helse-etterlevelse-topic",
    clientId: String = UUID.randomUUID().toString().slice(1..5),
    private val run: Consumer.(records: ConsumerRecords<String, String>) -> Unit = {}
) {
    private val consumer = KafkaConsumer(config.consumerConfig(clientId, config.consumerGroup), StringDeserializer(), StringDeserializer())
    private val running = AtomicBoolean(false)
    private val logger = LoggerFactory.getLogger(Consumer::class.java)
    internal fun isRunning() = running.get()
    private fun consumeMessages() {
        var lastException: Exception? = null
        try {
            consumer.subscribe(listOf(etterlevelseTopic))
            while (running.get()) {
                consumer.poll(Duration.ofSeconds(1)).also { records ->

                    records.forEach {
                        handleMessages(it.value())
                    }
                    run(records)
                    //participant.messages().forEach { publish(it.json()) }
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
        logger.info("starting QuizRapid")
        if (running.getAndSet(true)) return logger.info("QuizRapid already started")
        consumeMessages()
    }

    fun stop() {
        logger.info("stopping QuizRapid")
        if (!running.getAndSet(false)) return logger.info("rapid already stopped")
        consumer.wakeup()
    }

    private fun handleMessages(value: String) {
        TODO()

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