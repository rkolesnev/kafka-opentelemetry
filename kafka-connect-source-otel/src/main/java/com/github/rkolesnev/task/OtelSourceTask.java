package com.github.rkolesnev.task;

import com.github.rkolesnev.OtelUtils;
import com.github.rkolesnev.Version;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.source.SourceTaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtelSourceTask extends SourceTask {

  private static final Logger logger = LoggerFactory.getLogger(OtelSourceTask.class);

  private OtelSourceTaskConfig config;
  private String topic;
  private int numRecords;

  private final AtomicBoolean stopping = new AtomicBoolean(false);

  private final AtomicBoolean returnedPoll = new AtomicBoolean(false);

  @Override
  public String version() {
    return Version.VERSION;
  }

  private boolean telemetryConfigured = false;

  @Override
  public void initialize(SourceTaskContext context){
    super.initialize(context);
    //if (!telemetryConfigured) {
    //configureOpenTelemetry();
    //  telemetryConfigured = true;
    //}
  }
  @Override
  public void start(Map<String, String> properties) {
    try {
      config = new OtelSourceTaskConfig(properties);
    } catch (ConfigException e) {
      throw new ConnectException("Couldn't start OtelSourceTask due to configuration error", e);
    }
    numRecords = config.getInt(OtelSourceTaskConfig.NUM_RECORDS);
    topic = config.getString(OtelSourceTaskConfig.TOPIC);

  }

  private static void configureOpenTelemetry() {
    logger.info("About to register OpenTelemetry");
    //if (GlobalOpenTelemetry.get() == OpenTelemetry.noop()) {
    //  logger.info("GlobalOpenTelemetry is noop - registering new");

      Resource resource = Resource.getDefault()
          .merge(Resource.create(
              Attributes.of(ResourceAttributes.SERVICE_NAME, "otel-source-connector")));

      SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
          .addSpanProcessor(BatchSpanProcessor.builder(
                  JaegerGrpcSpanExporter.builder().setEndpoint("http://localhost:14250").build())
              .build())
          .setSampler(Sampler.alwaysOn())
          .setResource(resource)
          .build();

      OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
          .setTracerProvider(sdkTracerProvider)
          .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
          .buildAndRegisterGlobal();
  //  } else {
  //    logger.info("GlobalOpenTelemetry is not noop - skipping");
  //  }
  }

  //will be called by connect with a different thread than the stop thread
  @Override
  public List<SourceRecord> poll() {
    List<SourceRecord> results = new ArrayList<>();
    try {
      if (!returnedPoll.get()) {
        for (int i = 0; i < this.numRecords; i++) {
          if (!stopping.get()) {
            logger.info("generating records");
            //create record
            // List<Header> headers = new ArrayList<>();
            ConnectHeaders headers = new ConnectHeaders();
            Span span = OtelUtils.startSpan("generate-record", Context.current(), null);
            String traceId;
            try (Scope ignored = span.makeCurrent()) {
              logger.info("GlobalOpenTelemetry {}", GlobalOpenTelemetry.get());
              logger.info("GlobalOpenTelemetry Propagators {}",
                  GlobalOpenTelemetry.get().getPropagators());
              logger.info("Context current {}", Context.current());
              logger.info("GlobalOpenTelemetry Tracer {}",
                  GlobalOpenTelemetry.get().getTracer("io.opentelemetry.kafka-clients-2.6"));
              traceId = OtelUtils.traceIdStringFromContext(Context.current());
              span.end();
            }
            headers.add("traceparent",
                new SchemaAndValue(Schema.STRING_SCHEMA, traceId != null ? traceId : "null"));
            SourceRecord sourceRecord = new SourceRecord(
                Collections.singletonMap("source", "generator_" + topic),
                Collections.singletonMap("test", i), topic, 0, Schema.STRING_SCHEMA, "K" + i,
                Schema.STRING_SCHEMA, "V" + i, System.currentTimeMillis(), headers);
            //convert to SourceRecord

            logger.info("generated {}", sourceRecord);
            results.add(sourceRecord);
          }
        }
        returnedPoll.set(true);
      }
      if (results.isEmpty()) {
        logger.info("done emitting records, sleeping for 1 second");
        Thread.sleep(1000);
      }

    } catch (Exception e) {
      logger.error("error", e);
    }
    return results;
  }

  //will be called by connect with a different thread than poll thread
  public void stop() {
    stopping.set(true);
  }
}
