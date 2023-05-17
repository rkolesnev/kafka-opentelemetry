package com.github.rkolesnev;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingProducerInterceptor;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendedProducerInterceptor<K, V> extends TracingProducerInterceptor<K, V> {
  private static final Logger logger = LoggerFactory.getLogger(ExtendedProducerInterceptor.class);

  private static final KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());

  public ExtendedProducerInterceptor() {
    super();
  }

  public ProducerRecord<K, V> onSend(ProducerRecord<K, V> producerRecord) {
    if (producerRecord.headers().lastHeader("traceparent") != null) {
      logger.info("Header: {}", producerRecord.headers().lastHeader("traceparent"));
      String traceParent = new String(producerRecord.headers().lastHeader("traceparent").value(),
          StandardCharsets.UTF_8);
      Context parentContext = OtelUtils.contextFromTraceIdString(traceParent);
      Span span = OtelUtils.startSpan("connect-send", parentContext, null);
      try(Scope ignored = span.makeCurrent()) {
        ProducerRecord<K, V> returned = super.onSend(producerRecord);
        span.end();
        return returned;
      }
    } else {
      return super.onSend(producerRecord);
    }
  }
}
