package no.nav.helse

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.slf4j.LoggerFactory
import java.util.*


class Config(
    val appName: String,
    val bootstrapServers: List<String>,
    val topic: String,
    val consumerGroup: String,
    private val kafkaTrustStorePath: String?,
    private val kafkaKeyStorePath: String?,
    private val credStorePassword: String?
) {
    companion object {
        fun fromEnv(): Config {
            val appName = System.getenv("NAIS_APP_NAME")
            return Config(
                appName,
                System.getenv("KAFKA_BROKERS").split(";"),
                System.getenv("SUBSUMSJON_TOPIC"),
                System.getenv("SUBSUMSJON_CONSUMER_GROUP") ?: "consumer-$appName-v2",
                System.getenv("KAFKA_TRUSTSTORE_PATH"),
                System.getenv("KAFKA_KEYSTORE_PATH"),
                System.getenv("KAFKA_CREDSTORE_PASSWORD")
            )
        }
    }

    internal fun consumerConfig(clientId: String, consumerGroup: String) = Properties().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup)
        put(ConsumerConfig.CLIENT_ID_CONFIG, "consumer-$appName-$clientId")
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)

        if (kafkaKeyStorePath != null) {
            this += sslConfig()
        }

    }

    private fun sslConfig() = Properties().apply {
        LoggerFactory.getLogger("RapidConfig").info("SSL config enabled")
        put("security.protocol", "SSL")
        put("ssl.truststore.location", kafkaTrustStorePath!!)
        put("ssl.truststore.password", credStorePassword!!)
        put("ssl.keystore.type", "PKCS12")
        put("ssl.keystore.location", kafkaKeyStorePath!!)
        put("ssl.keystore.password", credStorePassword)
        put("ssl.key.password", credStorePassword)
    }

}