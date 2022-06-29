package no.nav.helse

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.*


class Config(
    val appName: String,
    val bootstrapServers: List<String>,
    val quizTopic: String,
    val consumerGroup: String,
    val autoCommit: Boolean,
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
                System.getenv("QUIZ_TOPIC") ?: "quiz-rapid",
                System.getenv("QUIZRAPID_CONSUMER_GROUP") ?: "consumer-$appName-v1",
                System.getenv("QUIZRAPID_AUTO_COMMIT").toBoolean(),
                System.getenv("KAFKA_TRUSTSTORE_PATH"),
                System.getenv("KAFKA_KEYSTORE_PATH"),
                System.getenv("KAFKA_CREDSTORE_PASSWORD")
            )
        }
    }

    internal fun consumerConfig(clientId: String, consumerGroup: String, autoCommit: Boolean) = Properties().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup)
        put(ConsumerConfig.CLIENT_ID_CONFIG, "consumer-$appName-$clientId")
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit.toString())
        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1")
        //put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "$maxPollIntervalMs")
        /*
        if (kafkaKeyStorePath != null) {
            this += sslConfig()
        }
        */
    }

}