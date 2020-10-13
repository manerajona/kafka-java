package kafka.tutorial2;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer implements Runnable {

    Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());

    // use your own credentials - don't share them with anyone
    String consumerKey = "";
    String consumerSecret = "";
    String token = "";
    String secret = "";

    List<String> terms = Lists.newArrayList("bitcoin", "usa", "politics", "sport", "soccer");

    @Override
    public void run() {

        logger.info("Setup");

        /* Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
        BlockingQueue<String> msgQueue;
        msgQueue = new LinkedBlockingQueue<>(1000);

        // create a twitter client
        Client client = createTwitterClient(msgQueue);
        // Attempts to establish a connection.
        client.connect();

        // create a kafka producer
        KafkaProducer<String, String> producer = createKafkaProducer();

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("shutting down client from twitter...");
            client.stop();
            logger.info("closing producer...");
            producer.close();
            logger.info("done!");
        }));

        // loop to send tweets to kafka
        // on a different thread, or multiple different threads....
        while (!client.isDone()) {
            String msg;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
                logger.info("Message: {}", msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
                client.stop();
                msg = null;
            }
            if (msg != null) {
                // kafka-topics.sh --zookeeper 127.0.0.1:2181 --create --topic twitter_tweets --partitions 6 --replication-factor 1
                // kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic twitter_tweets
                producer.send(new ProducerRecord<>("twitter_tweets", null, msg), (recordMetadata, exception) -> {
                    if (exception != null) logger.error("Something bad happened", exception);
                });
            }
        }
        logger.info("End of application");
    }

    Client createTwitterClient(BlockingQueue<String> msgQueue) {

        /* Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hbHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hbEndpoint = new StatusesFilterEndpoint();

        hbEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hbAuth = new OAuth1(consumerKey, consumerSecret, token, secret);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")                              // optional: mainly for the logs
                .hosts(hbHosts)
                .authentication(hbAuth)
                .endpoint(hbEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        return builder.build();
    }

    KafkaProducer<String, String> createKafkaProducer() {
        String bootstrapServers = "127.0.0.1:9092";

        // create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create safe Producer
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, Boolean.TRUE.toString());
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5"); // kafka 2.0 >= 1.1 so we can keep this as 5. Use 1 otherwise.

        // high throughput producer (at the expense of a bit of latency and CPU usage)
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024)); // 32 KB batch size

        // create the producer
        return new KafkaProducer<>(properties);
    }

    public static void main(String[] args) {
        new TwitterProducer().run();
    }
}
