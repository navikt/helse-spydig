package no.nav.helse

import io.ktor.server.engine.ApplicationEngine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ConsumerRunner(
    config: Config,
    builder: () -> ApplicationEngine,
) {

    private val consumer = Consumer(config, config.topic)
    private val ktor = builder()

    fun startBlocking() {
        runBlocking { start() }
    }

    suspend fun start() {
        val ktorServer = ktor.start(false)
        try {
            coroutineScope {
                launch { consumer.start() }
            }
        } finally {
            val gracePeriod = 5000L
            val forcefulShutdownTimeout = 30000L
            logger.info("shutting down ktor, waiting $gracePeriod ms for workers to exit. Forcing shutdown after $forcefulShutdownTimeout ms")
            ktorServer.stop(gracePeriod, forcefulShutdownTimeout)
            logger.info("ktor shutdown complete: end of life. goodbye.")
        }
    }
}