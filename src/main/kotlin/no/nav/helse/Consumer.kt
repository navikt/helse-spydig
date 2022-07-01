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
        private val requests = Counter.build()
            .name("spydig_validation_errors").help("Total errors.").register()
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
        val message = objectMapper.readTree(value)
        if (!validator.isJSONvalid(message)) {
            requests.inc()
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