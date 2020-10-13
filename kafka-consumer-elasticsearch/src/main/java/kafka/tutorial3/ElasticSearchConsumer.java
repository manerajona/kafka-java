package kafka.tutorial3;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ElasticSearchConsumer {

    public static final Logger logger = LoggerFactory.getLogger(ElasticSearchConsumer.class.getName());

    public static RestHighLevelClient createClient() {

        // todo IF YOU USE LOCAL ELASTICSEARCH
        //  String hostname = "localhost";
        //  RestClientBuilder builder = RestClient.builder(new HttpHost(hostname,9200,"http"));

        // replace with your own credentials
        String hostname = ""; // localhost or bonsai url
        String username = "";
        String password = "";

        // todo Create index: PUT /twitter
        //  check GET /_cat/indices?v

        // credentials provider help supply username and password
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);

        RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, 443, "https"))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));

        return new RestHighLevelClient(builder);
    }

    public static KafkaConsumer<String, String> createConsumer(String topic) {

        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "kafka-demo-elasticsearch";

        // create consumer configs
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // disable auto commit of offsets
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100"); // disable auto commit of offsets

        // create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singleton(topic));

        return consumer;

    }

    private static final JsonParser jsonParser = new JsonParser();

    private static String extractIdFromTweet(String tweetJson) {
        // gson library
        return jsonParser.parse(tweetJson)
                .getAsJsonObject()
                .get("id_str")
                .getAsString();
    }

    public static void main(String[] args) throws IOException {

        RestHighLevelClient client = createClient();

        KafkaConsumer<String, String> consumer = createConsumer("twitter_tweets");

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100)); // new in Kafka 2.0.0

            int recordCount = records.count();
            logger.info("Received " + recordCount + " records");

            BulkRequest bulkRequest = new BulkRequest();

            for (var record : records) {
                try {
                    /* Kafka generic ID
                    String id = record.topic() + "_" + record.partition() + "_" + record.offset();
                     */

                    String id = extractIdFromTweet(record.value());

                    /* Uncomment this code if you are using elastic search version < 7.0
                     IndexRequest indexRequest = new IndexRequest(
                     "twitter",
                     "tweets",
                     id // this is to make our consumer idempotent!!!
                     ).source(record.value(), XContentType.JSON);
                     */

                    IndexRequest indexRequest = new IndexRequest("tweets")
                            .source(record.value(), XContentType.JSON)
                            .id(id); // this is to make our consumer idempotent!!!

                    bulkRequest.add(indexRequest); // we add to our bulk request (takes no time)
                } catch (NullPointerException e) {
                    logger.warn("skipping bad data: " + record.value());
                }
            }

            if (recordCount > 0) {
                BulkResponse bulkItemResponses = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                logger.info("Committing offsets...");
                consumer.commitSync();
                logger.info("Offsets have been committed");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }

        // close the client gracefully
        // client.close();

    }
}
