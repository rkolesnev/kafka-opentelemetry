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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.header.Header;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtelSinkTask extends SinkTask {

  private static final Logger logger = LoggerFactory.getLogger(OtelSinkTask.class);

  private OtelSinkTaskConfig config;

  private final AtomicBoolean stopping = new AtomicBoolean(false);

  @Override
  public String version() {
    return Version.VERSION;
  }

  @Override
  public void start(Map<String, String> properties) {
    try {
      config = new OtelSinkTaskConfig(properties);
    } catch (ConfigException e) {
      throw new ConnectException("Couldn't start OtelSinkTask due to configuration error", e);
    }
  }

  @Override
  public void put(Collection<SinkRecord> records) {
    if (stopping.get()) {
      return;
    }
    List<Span> putSpans = new ArrayList<>();
    records.forEach(sinkRecord -> {
      Header tracingHeader = sinkRecord.headers().lastWithName("traceparent");
      if (tracingHeader != null) {
        logger.info("Tracing header {}", tracingHeader.value());
        Context parentContext = OtelUtils.contextFromTraceIdString(
            tracingHeader.value().toString());
        Span span = OtelUtils.startSpan("sink-put", parentContext,(String) null);
        logger.info("Sink received message: {}", sinkRecord);
        putSpans.add(span);
      }
    });
    Span batchSpan = OtelUtils.startSpan("out-send", Context.current(), putSpans);
    try (Scope ignored = batchSpan.makeCurrent()) {
      logger.info("Sink pushed batch: {}", records.size());
      batchSpan.end();
    }
    putSpans.forEach(putSpan -> {
      try (Scope ignored = batchSpan.makeCurrent()) {
        putSpan.end();
      }
    });
  }

//will be called by connect with a different thread than poll thread
public void stop(){
    stopping.set(true);
    }
    }
