package com.github.rkolesnev.kafka.opentelemetry;

import com.github.rkolesnev.kafka.opentelemetry.Constants.Topics;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


//-javaagent:path/to/opentelemetry-javaagent.jar -Dotel.service.name=my-kafka-service -Dotel.traces.exporter=jaeger -Dotel.metrics.exporter=none -Dotel.instrumentation.messaging.experimental.receive-telemetry.enabled=true
public class ConsumerProducerService {

  private static final Logger log = LogManager.getLogger(ConsumerProducerService.class);
  private static final AtomicBoolean running = new AtomicBoolean(false);

  private static void configureTelemetry() {
    Resource resource = Resource.getDefault()
        .merge(Resource.create(
            Attributes.of(ResourceAttributes.SERVICE_NAME, "ServiceB-wrap")));

    SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(
            JaegerGrpcSpanExporter.builder().build()).build())
        .setSampler(Sampler.alwaysOn())
        .setResource(resource)
        .build();

    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .buildAndRegisterGlobal();
  }

  public static void main(String[] args) {
    running.set(true);
    configureTelemetry();
    KafkaTelemetry kafkaTelemetry = KafkaTelemetry.builder(GlobalOpenTelemetry.get()).setCaptureExperimentalSpanAttributes(true).build();
    Producer<String, String> producer = kafkaTelemetry.wrap(
        new KafkaProducer<>(Common.getProducerProperties()));
    Consumer<String, String> consumer = kafkaTelemetry.wrap(
        new KafkaConsumer<>(Common.getConsumerProperties()));

    consumer.subscribe(Collections.singleton(Topics.TopicA));
    try {
      log.info("Polling ...");
      while (running.get()) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : records) {
          log.info("Received message key = [{}], value = [{}], offset = [{}]", record.key(),
              record.value(), record.offset());
          producer.send(new ProducerRecord<>(Topics.TopicB, record.key(), record.value()));
          log.info("Send message key = [{}], value = [{}], topic=[{}]", record.key(),
              record.value(),
              Topics.TopicB);
        }
      }
    } catch (WakeupException we) {
      // Ignore exception if closing
      if (running.get()) {
        throw we;
      }
    } finally {
      consumer.close();
      producer.close();
    }

    Runtime.getRuntime().addShutdownHook(new Thread("consumer-producer-shutdown-hook") {
      @Override
      public void run() {
        running.set(false);
        consumer.close();
        producer.close();
      }
    });
  }
}
