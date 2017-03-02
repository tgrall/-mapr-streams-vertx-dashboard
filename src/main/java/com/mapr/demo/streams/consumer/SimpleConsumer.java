package com.mapr.demo.streams.consumer;


import com.mapr.demo.streams.ConfigConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleConsumer extends AbstractVerticle {
  public static final String EVENTBUS_DEFAULT_ADDRESS = "kafka.message.consumer";
  public static final int DEFAULT_POLL_MS = 100;
  private String busAddress;
  private static final Logger logger = LoggerFactory.getLogger(SimpleConsumer.class);
  private EventBus bus;

  private AtomicBoolean running;
  private KafkaConsumer consumer;
  private List<String> topics;
  private JsonObject verticleConfig;
  private ExecutorService backgroundConsumer;
  private int pollIntervalMs;

  @Override
  public void start(final Future<Void> startedResult) {

    try {
      bus = vertx.eventBus();
      running = new AtomicBoolean(true);

      verticleConfig = config();
      Properties kafkaConfig = populateKafkaConfig(verticleConfig);
      JsonArray topicConfig = verticleConfig.getJsonArray(ConfigConstants.TOPICS);

      busAddress = verticleConfig.getString(ConfigConstants.EVENTBUS_ADDRESS, EVENTBUS_DEFAULT_ADDRESS);
      pollIntervalMs = verticleConfig.getInteger(ConfigConstants.CONSUMER_POLL_INTERVAL_MS, DEFAULT_POLL_MS);

      Runtime.getRuntime().addShutdownHook(new Thread() {
        // try to disconnect from ZK as gracefully as possible
        public void run() {
          shutdown();
        }
      });

      backgroundConsumer = Executors.newSingleThreadExecutor();
      backgroundConsumer.submit(() -> {
        try {
          consumer = new KafkaConsumer(kafkaConfig);

          topics = new ArrayList<>();
          for (int i = 0; i < topicConfig.size(); i++) {
            topics.add(topicConfig.getString(i));
            logger.info("Subscribing to topic ");
          }

          // signal success before we enter read loop
          startedResult.complete();
          consume();
        } catch (Exception ex) {
          String error = "Failed to startup";
          logger.error(error, ex);
          bus.send(ConfigConstants.CONSUMER_ERROR_TOPIC, getErrorString(error, ex.getMessage()));
          startedResult.fail(ex);
        }
      });
    } catch (Exception ex) {
      String error = "Failed to startup";
      logger.error(error, ex);
      bus.send(ConfigConstants.CONSUMER_ERROR_TOPIC, getErrorString("Failed to startup", ex.getMessage()));
      startedResult.fail(ex);
    }
  }

  private String getErrorString(String error, String errorMessage) {
    return String.format("%s - error: %s", error, errorMessage);
  }

  /**
   * Handles looping and consuming
   */
  private void consume() {
    consumer.subscribe(topics);
    while (running.get()) {
      try {
        ConsumerRecords records = consumer.poll(pollIntervalMs);

        // there were no messages
        if (records == null) { continue; }

        Iterator<ConsumerRecord<String,String>> iterator = records.iterator();

        // roll through and put each kafka message on the event bus
        while (iterator.hasNext()) { sendMessage(iterator.next()); }

      } catch (Exception ex) {
        String error = "Error consuming messages from kafka";
        logger.error(error, ex);
        bus.send(ConfigConstants.CONSUMER_ERROR_TOPIC, getErrorString(error, ex.getMessage()));
      }
    }
  }

  @Override
  public void stop() { running.compareAndSet(true, false); }

  /**
   * Send the inbound message to the event bus consumer.
   *
   * @param record the kafka event
   */
  private void sendMessage(ConsumerRecord<String, String> record) {
    try { bus.send(busAddress, KafkaEvent.createEventForBus(record)); }
    catch (Exception ex) {
      String error = String.format("Error sending messages on event bus - record: %s", record.toString());
      logger.error(error, ex);
      bus.send(ConfigConstants.CONSUMER_ERROR_TOPIC, getErrorString(error, ex.getMessage()));
    }
  }

  private Properties populateKafkaConfig(JsonObject config) {
    Properties consumerConfig = new Properties();

    consumerConfig.put(ConfigConstants.AUTO_OFFSET_RESET,
            config.getString(ConfigConstants.AUTO_OFFSET_RESET, "earliest"));



    consumerConfig.put(ConfigConstants.KEY_DESERIALIZER_CLASS,
            config.getString(ConfigConstants.KEY_DESERIALIZER_CLASS, ConfigConstants.DEFAULT_DESERIALIZER_CLASS));
    consumerConfig.put(ConfigConstants.VALUE_DESERIALIZER_CLASS,
            config.getString(ConfigConstants.VALUE_DESERIALIZER_CLASS, ConfigConstants.DEFAULT_DESERIALIZER_CLASS));
    consumerConfig.put(ConfigConstants.GROUP_ID, getRequiredConfig(ConfigConstants.GROUP_ID));


    return consumerConfig;
  }

  private String getRequiredConfig(String key) {
    String value = verticleConfig.getString(key, null);

    if (null == value) {
      throw new IllegalArgumentException(String.format("Required config value not found key: %s", key));
    }
    return value;
  }

  /**
   * Handle stopping the consumer.
   */
  private void shutdown() {
    running.compareAndSet(true, false);
    try {
      if(consumer != null) {
        try {
          consumer.unsubscribe();
          consumer.close();
          consumer = null;
        } catch (Exception ex) {  }
      }

      if(backgroundConsumer != null) {
        backgroundConsumer.shutdown();
        backgroundConsumer = null;
      }
    } catch (Exception ex) {
      logger.error("Failed to close consumer", ex);
    }
  }
}