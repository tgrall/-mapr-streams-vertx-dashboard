package com.mapr.demo;


import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

public class WebServer {

  public static String ekgTopic;

  public static void main(String[] args) throws Exception {


    if (args.length != 2) {
      throw new IllegalArgumentException("Must have the HtttPort and Topic :  8080 /apps/iot_stream:ecg  ");
    }
    int httpPort = Integer.parseInt(args[0]);
    ekgTopic = args[1];



    Vertx vertx = Vertx.vertx();

    Router router = Router.router(vertx);

    BridgeOptions options = new BridgeOptions();
    options.setOutboundPermitted(Collections.singletonList(new PermittedOptions().setAddress("dashboard")));
    router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(options));
    router.route().handler(StaticHandler.create().setCachingEnabled(false));

    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(router::accept).listen(httpPort, ar -> {
      if (ar.succeeded()) {
        System.out.println("Http server started started on port "+ httpPort);
      } else {
        ar.cause().printStackTrace();
      }
    });


    // Create a MapR Streams Consumer
    KafkaConsumer<String, String> consumer;
    Properties properties = new Properties();
    properties.setProperty("group","vertx_dashboard");
    properties.setProperty("enable.auto.commit","true");
    properties.setProperty("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
    properties.setProperty("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");

    consumer = new KafkaConsumer<>(properties);

    consumer.subscribe(Arrays.asList(ekgTopic));
    System.out.println(ekgTopic);
    while (true) {
      // read records with a short timeout. If we time out, we don't really care.
      ConsumerRecords<String, String> records = consumer.poll(200);
      for (ConsumerRecord<String, String> record : records) {
        //System.out.println(record.value());
        vertx.eventBus().publish("dashboard",  record.value() );
      }
    }



  }
}
