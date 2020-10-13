# kafka-java

## Basics

    Integrations:
        Protocol - TCP, HTTP, REST, FTP, JDBC
        Data format - Binary, CSV, JSON, XML
        Data schema and evolution (how data si shaped and may change)

    Scalability:
        Can scale to 100s of brokers
        Can scale to million of messages per sec
        High performance - real time (less 10ms latency)
        Used by +40% of Furtune 500

    Use cases:
        Messaging system
        Activity tracking
        Gather metrics
        Application logs
        Stream processing (Kafka streams or Spark)
        De-coupling of a system dependencies
        Big Data integration (Spark, Hadoop...)

    Topic:
        A particular stream of data
        Similar to a table in DB
        You can have as many topics you want
        A topic is identified by its name
        Topics are spit in PARTITIONS, identified by its Offset
	
	Partitions:
		Each patition have an id (OFFSET)
		Each parition is ordered (no guaranteed across them)
		Messages in partitions are immutable
		Messages are randomly assigned to a partition unless a key is provided
		Data is kepf for a limited time (default is one week)

    Broker:
        A kafka cluster is composed by multiple brokers (servers)
        Each broker is identified by an id (Integer)
        Each broker contains certain topic partitions
        A good number to start is 3 brokers

    Topic Replication (HA):
        Topic should have a replication factor (usualy 2 or 3)
        If a broker is down another one can serve the data
        Replication factor can not be lager than available brokers
        Each broker should be a LEADER for a Partition
        Only one broker can receive and serve data for a partition at the same time
        Each partition has multiple ISR (in-sync replica) but only one Leader

    Producers:
        Producers write data to topics
        Producers know which broker and partition to write to
        Only need to connect to one broker (any broker) and just provide the topic name you want to write to. 
        Kafka Clients will route your data to the appropriate brokers and partitions for you!
        In case of failures, Producers will automatically recover
        Producers send data in Round-robin (the load is balanced)
        Producers can choose to receive Acknowledgment (ACKS)
            acks=0 	 : Producer wont wait for acknowledgment (possible data loss)
            acks=1 	 : Producer will wait for leader acknowledgment (limited data loss)
            acks=all : Leader + replicas acks (no data loss)

	Messages Keys:
		Producers can choose to send a key with the message (string, number)
		If key is null data is sent in round robin fashion
		If key is sent all messages with that key will go to the same Partition

    Consumers:
        Read data from a topic
        Consumers know which broker to read
        In case of broker failure consumers know how to recover
        Data is read in order within each partitions
        Consumers read data in consumer group, each group reads from (mutually) exclusive partitions
        Consumers will automatically use a GroupCoordinator and ConsumerCoordinator to being assing to a partition
        
	Offsets:
		Kafka stores the offset at which a consumer group has been reading
		Offsets are commited in Topic named _consumer_offsets
		Consumer should be commiting the Offsets when data is received in a group
		If a consumer dies it will be able to read back where it left thanks to commited offsets

		Delivery sematics:
			- At most once (offsets are commited as soon as the msg is received)
			- At least once (offsets are commited after the msg is processed)
			- Exacty once (only for kafka streams API)

    Broker discovery:
        Each broker is a BOOTSTRAP SERVER
        Each broker knows about all brokers, topics and partitions

    Zookeeper:
        KAFKA CANT WORK WITHOUT ZOOKEEPER
        Manages brokers and keep a list of them
        helps in leader election for paritions
        Sends notifications to Kafka in case of changes
        Zookeepers has a leaders (handle writes) and followers (handle reads)
        Zookeeper DOES NOT store consumer offsets

## Client Bi-Directional Compatibility
Means than an OLDER client can talk to a NEWER Broker (2.0)
And a NEWER Client can talk to an OLDER Broker (1.1)


## Installing Kafka in Linux
```sh
$ java --version
openjdk 11.0.7 2020-04-14 LTS
OpenJDK Runtime Environment Corretto-11.0.7.10.1 (build 11.0.7+10-LTS)
OpenJDK 64-Bit Server VM Corretto-11.0.7.10.1 (build 11.0.7+10-LTS, mixed mode)
```
```sh
$ wget http://www-us.apache.org/dist/kafka/2.4.0/kafka_2.13-2.4.0.tgz
$ tar xzf kafka_2.13-2.4.0.tgz
$ sudo mv kafka_2.13-2.4.0 /usr/local/kafka
$ /usr/local/kafka/bin/kafka-topics.sh 
```
```sh
$ cd /usr/local/kafka/
$ pwd
/usr/local/kafka/
```
(Optional)
```sh
$ nano ~/.bashrc 
```sh
# Kafka
export PATH=/usr/local/kafka/bin:$PATH
```
```sh
$ source ~/.bashrc
```
```sh
$ echo $PATH
/usr/local/kafka/bin:...
```
```sh
$ kafka-topics.sh

$ kafka-
kafka-acls.sh                        kafka-consumer-groups.sh             kafka-leader-election.sh             kafka-reassign-partitions.sh         kafka-streams-application-reset.sh
kafka-broker-api-versions.sh         kafka-consumer-perf-test.sh          kafka-log-dirs.sh                    kafka-replica-verification.sh        kafka-topics.sh
kafka-configs.sh                     kafka-delegation-tokens.sh           kafka-mirror-maker.sh                kafka-run-class.sh                   kafka-verifiable-consumer.sh
kafka-console-consumer.sh            kafka-delete-records.sh              kafka-preferred-replica-election.sh  kafka-server-start.sh                kafka-verifiable-producer.sh
kafka-console-producer.sh            kafka-dump-log.sh                    kafka-producer-perf-test.sh          kafka-server-stop.sh  
```

### Start Zookeeper
```sh
/usr/local/kafka$ mkdir data
/usr/local/kafka$ mkdir data/zookeeper
/usr/local/kafka$ nano config/zookeeper.properties
...
# the directory where the snapshot is stored.
dataDir=/usr/local/kafka/data/zookeeper

/usr/local/kafka$ zookeeper-server-start.sh config/zookeeper.properties
/usr/local/kafka$ tree data
data
└── zookeeper
    └── version-2
        └── snapshot.0
```

### Start Kafka
```sh
/usr/local/kafka$ nano config/server.properties
/usr/local/kafka$ mkdir data/kafka

...
############################# Log Basics #############################

# A comma separated list of directories under which to store log files
log.dirs=/usr/local/kafka/data/kafka


/usr/local/kafka$ kafka-server-start.sh config/server.properties 

```

## CLI (Command Line Inteface)
```bash
# help
$ kafka-topics.sh

# create new topic
$ kafka-topics.sh --zookeeper 127.0.0.1:2181 --topic new_topic --create --partitions 3 --replication-factor 1
WARNING: Due to limitations in metric names, topics with a period ('.') or underscore ('_') could collide. To avoid issues it is best to use either, but not both.
Created topic new_topic.

# list all topics
$ kafka-topics.sh --zookeeper 127.0.0.1:2181 --list
new_topic

# describe topic (Leader 0 means that the partition is the actual lider)
$ kafka-topics.sh --zookeeper 127.0.0.1:2181 --topic new_topic --describe
Topic: new_topic        PartitionCount: 3       ReplicationFactor: 1    Configs: 
        Topic: new_topic        Partition: 0    Leader: 0      Replicas: 0      Isr: 0
        Topic: new_topic        Partition: 1    Leader: 0      Replicas: 0      Isr: 0
        Topic: new_topic        Partition: 2    Leader: 0      Replicas: 0      Isr: 0

# delete topic
$ kafka-topics.sh --zookeeper 127.0.0.1:2181 --topic other_topic --delete
Topic other_topic is marked for deletion.
Note: This will have no impact if delete.topic.enable is not set to true.

# help
$ kafka-console-producer.sh

# create producer
$ kafka-console-producer.sh --broker-list 127.0.0.1:9092 --topic new_topic
>hello world
>yeah!!

# create producer with acks
$ kafka-console-producer.s-broker-list 127.0.0.1:9092 --topic new_topic --producer-property acks=all
>hello
>hi!
>kafka is cool

# create producer with keys
$ kafka-console-producer.sh --broker-list 127.0.0.1:9092 --topic first_topic --property parse.key=true --property key.separator=,
> key,value
> another key,another value

# help
$ kafka-console-consumer.sh

# create consumer
$ kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic new_topic --from-beginning
hello

# create consumer with keys
$ kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic first_topic --from-beginning --property print.key=true --property key.separator=,

# consume from beginnig
$ kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic new_topic --from-beginning
hi!
yeah!!
hello
hello world
kafka is cool

# consuming in groups (round roibin)
$ kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic new_topic --group my-group

# consumer groups
$ kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 --list
$ kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 --describe --group my-group

# reseting offsets
$ kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 --group my-group --reset-offsets --to-earliest --execute --topic new_topic

# shift offsets
$ kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 --group my-group --reset-offsets --shift-by 2 --execute --topic new_topic
$ kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 --group my-group --reset-offsets --shift-by -2 --execute --topic new_topic
```
