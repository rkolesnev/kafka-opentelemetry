package com.github.rkolesnev;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import java.util.Arrays;
import java.util.List;

public class OtelUtils {

  public static String traceIdStringFromContext(Context context) {
    String[] traceIdentifierHolder = new String[1];
    GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator()
        .inject(context, traceIdentifierHolder, StringTextMapSetter.getInstance());
    return traceIdentifierHolder[0];
  }

  public static Span startSpan(String name, Context parent, String linkedSpanId) {
    SpanBuilder spanBuilder = GlobalOpenTelemetry.get()
        .getTracer("io.opentelemetry.kafka-clients-2.6")
        .spanBuilder(name).setParent(parent);
    if (linkedSpanId != null) {
      SpanContext linkedSpanContext = Span.fromContext(contextFromTraceIdString(linkedSpanId))
          .getSpanContext();
      spanBuilder.addLink(linkedSpanContext);
    }

    return spanBuilder.startSpan();
  }

  public static Span startSpan(String name, Context parent, List<Span> linkedSpans) {
    SpanBuilder spanBuilder = GlobalOpenTelemetry.get()
        .getTracer("io.opentelemetry.kafka-clients-2.6")
        .spanBuilder(name).setParent(parent);
    if (linkedSpans != null) {
      linkedSpans.forEach(linkedSpan -> {
        SpanContext linkedSpanContext = linkedSpan.getSpanContext();
        spanBuilder.addLink(linkedSpanContext);
      });
    }

    return spanBuilder.startSpan();
  }

  public static Context contextFromTraceIdString(String traceId) {
    return GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator()
        .extract(Context.current(), traceId,
            StringTextMapGetter.getInstance());
  }
}
