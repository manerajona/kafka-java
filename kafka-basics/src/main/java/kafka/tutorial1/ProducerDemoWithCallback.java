package kafka.tutorial1;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.stream.IntStream;

public class ProducerDemoWithCallback {

    public static void main(String[] args) {

        final Logger logger = LoggerFactory.getLogger(ProducerDemoWithCallback.class);

        String bootstrapServers = "127.0.0.1:9092";

        // create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);


        IntStream.rangeClosed(0, 10).forEach(i -> {
                    // create a producer record
                    ProducerRecord<String, String> record =
                            new ProducerRecord<>("new_topic", String.format("hello world %d", i));

                    // send with callback
                    producer.send(record, (recordMetadata, exception) -> {
                        // executes every time a record is successfully sent or an exception is thrown
                        if (exception == null) {
                            // the record was successfully sent
                            logger.info("Received new metadata. \n" +
                                            "Topic:{}\n" +
                                            "Partition: {}\n" +
                                            "Offset: {}\n" +
                                            "Timestamp: {}",
                                    recordMetadata.topic(),
                                    recordMetadata.partition(),
                                    recordMetadata.offset(),
                                    recordMetadata.timestamp());
                        } else {
                            logger.error("Error while producing", exception);
                        }
                    });
                }
        );
        // flush data
        producer.flush();
        // flush and close producer
        producer.close();

        // kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic new_topic --group my-java-app
    }
}
