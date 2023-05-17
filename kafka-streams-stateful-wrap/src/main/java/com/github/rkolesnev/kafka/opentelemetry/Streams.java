package com.github.rkolesnev.kafka.opentelemetry;

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
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.streams.processor.internals.DefaultKafkaClientSupplier;

public class Streams extends BaseStreamsStatefulWrap {

    public static void main(String[] args) {
        configureOpenTelemetry();

        Streams streams = new Streams();
        streams.loadConfiguration(System.getenv());
        Properties props = streams.loadKafkaStreamsProperties();

        CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.stop();
                latch.countDown();
            }
        });

        try {
            streams.run(props, new TracingKafkaClientSupplier());
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
    }

    private static void configureOpenTelemetry() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "streams-stateless-wrap")));

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(JaegerGrpcSpanExporter.builder().build()).build())
                .setSampler(Sampler.alwaysOn())
                .setResource(resource)
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }
    private static class TracingKafkaClientSupplier extends DefaultKafkaClientSupplier {
        @Override
        public Producer<byte[], byte[]> getProducer(Map<String, Object> config) {
            KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
            return telemetry.wrap(super.getProducer(config));
        }

        @Override
        public Consumer<byte[], byte[]> getConsumer(Map<String, Object> config) {
            KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
            return telemetry.wrap(super.getConsumer(config));
        }

        @Override
        public Consumer<byte[], byte[]> getRestoreConsumer(Map<String, Object> config) {
            return this.getConsumer(config);
        }

        @Override
        public Consumer<byte[], byte[]> getGlobalConsumer(Map<String, Object> config) {
            return this.getConsumer(config);
        }
    }
}
