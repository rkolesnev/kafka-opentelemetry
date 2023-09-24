package com.example.javaagent.instrumentation.helpers;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;

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


  public static byte[] wrapValueWithTrace(byte[] value, byte[] trace) {
    if (value == null) {
      return null;
    }
    byte[] combined = Arrays.copyOf(value, value.length + trace.length);
    System.arraycopy(trace, 0, combined, value.length, trace.length);
    return combined;
  }

  public static Pair<byte[], byte[]> unWrapValueWithTrace(byte[] valueAndTrace) {
    if (valueAndTrace == null) {
      return null;
    }
    if (valueAndTrace.length < 56) {
      return Pair.of(valueAndTrace, null);
    }
    int traceLength = 55;
    byte[] payload = new byte[valueAndTrace.length - traceLength];
    System.arraycopy(valueAndTrace, 0, payload, 0, payload.length);
    byte[] trace = new byte[traceLength];
    System.arraycopy(valueAndTrace, payload.length, trace, 0, trace.length);
    return Pair.of(payload, trace);
  }

  public static byte[] createGetSpanAndReturnUnwrappedValue(byte[] bytesWithTrace, Instant startTs, String spanName){
    if (bytesWithTrace == null) {
      return null;
    } else {
      Pair<byte[], byte[]> valueAndTrace = unWrapValueWithTrace(bytesWithTrace);
      String storedSpanContext = null;
      if (valueAndTrace.getRight() != null) {
        storedSpanContext = new String(valueAndTrace.getRight(), StandardCharsets.UTF_8);
      }
      Span span = startSpan(spanName, Context.current(), storedSpanContext, startTs);
      Scope ignored = span.makeCurrent();
      span.end();
      ignored.close();
      return valueAndTrace.getLeft();
    }
  }
}
