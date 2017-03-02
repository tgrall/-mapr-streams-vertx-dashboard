package com.mapr.demo.streams;

public class ConfigConstants {
  public static String TOPICS = "topics";
  public static String GROUP_ID = "group.id";
  public static String BACKOFF_INCREMENT_MS = "backoff.increment.ms";
  public static String AUTO_OFFSET_RESET = "autooffset.reset";
  public static String EVENTBUS_ADDRESS = "eventbus.address";
  public static String CONSUMER_POLL_INTERVAL_MS = "consumer.poll.interval.ms";
  public static String ZK_CONNECT = "zookeeper.connect";
  public static String KEY_DESERIALIZER_CLASS = "key.deserializer";
  public static String VALUE_DESERIALIZER_CLASS = "value.deserializer";
  public static String DEFAULT_DESERIALIZER_CLASS = "org.apache.kafka.common.serialization.StringDeserializer";
  public static String CONSUMER_ERROR_TOPIC = "kafka.consumer.error";

  //common
  public static String BOOTSTRAP_SERVERS = "bootstrap.servers";

  // producer constants
  public static String PRODUCER_ERROR_TOPIC = "kafka.producer.error";
  public static String DEFAULT_TOPIC = "default.topic";
  public static String KEY_SERIALIZER_CLASS = "key.serializer";
  public static String PRODUCER_TYPE = "producer.type";
  public static String SERIALIZER_CLASS = "serializer.class";
  public static String VALUE_SERIALIZER_CLASS = "value.serializer";
  public static String DEFAULT_SERIALIZER_CLASS = "org.apache.kafka.common.serialization.StringSerializer";
  public static String MAX_BLOCK_MS = "max.block.ms";

  // event bus fields
  public static final String TOPIC_FIELD = "topic";
  public static final String KEY_FIELD = "key";
  public static final String VALUE_FIELD = "value";
  public static final String PARTITION_FIELD = "partition";
}
