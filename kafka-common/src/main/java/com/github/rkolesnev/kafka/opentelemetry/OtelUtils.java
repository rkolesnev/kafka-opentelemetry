package com.github.rkolesnev.kafka.opentelemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import java.time.Instant;

public class OtelUtils {

  public static String traceIdStringFromContext(Context context) {
    String[] traceIdentifierHolder = new String[1];
    GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator()
        .inject(context, traceIdentifierHolder, StringTextMapSetter.getInstance());
    return traceIdentifierHolder[0];
  }

  public static Span startSpan(String name, Context parent, String linkedSpanId, Instant startTs) {
    SpanBuilder spanBuilder = GlobalOpenTelemetry.get()
        .getTracer("io.opentelemetry.kafka-streams-0.11")
        .spanBuilder(name).setParent(parent);
    if (linkedSpanId != null) {
      SpanContext linkedSpanContext = Span.fromContext(contextFromTraceIdString(linkedSpanId))
          .getSpanContext();
      spanBuilder.addLink(linkedSpanContext);
    }
    if (startTs != null) {
      spanBuilder.setStartTimestamp(startTs);
    }
    return spanBuilder.startSpan();
  }

  public static Span startSpan(String name, Context parent, String linkedSpanId) {
    return startSpan(name, parent, linkedSpanId, null);
  }


  public static Context contextFromTraceIdString(String traceId) {
    return GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator()
        .extract(Context.current(), traceId,
            StringTextMapGetter.getInstance());
  }
}
