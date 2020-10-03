package kafka.tutorial1;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ConsumerDemoAssignSeek {

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(ConsumerDemoAssignSeek.class.getName());

        String bootstrapServers = "127.0.0.1:9092";
        String topic = "new_topic";

        // create consumer configs
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // assign and seek are mostly used to replay data or fetch a specific message
        TopicPartition partitionToReadFrom;
        long offsetToReadFrom;
        {
            partitionToReadFrom = new TopicPartition(topic, 0);
            consumer.assign(Collections.singleton(partitionToReadFrom));

            offsetToReadFrom = 15L;
        }
        consumer.seek(partitionToReadFrom, offsetToReadFrom);

        int numberOfMessagesToRead = 5, numberOfMessagesReadSoFar = 0;
        boolean keepOnReading = true;

        // poll for new data
        while (keepOnReading) {
            for (var record : consumer.poll(Duration.ofMillis(100))) {
                logger.info("Key: {}, Value: {}", record.key(), record.value());
                logger.info("Partition: {}, Offset:{}", record.partition(), record.offset());

                numberOfMessagesReadSoFar++;
                keepOnReading = numberOfMessagesReadSoFar < numberOfMessagesToRead; // to exit while loop
                if (!keepOnReading) break; // to exit the for loop
            }
        }
        logger.info("Exiting the application");

    }
}
