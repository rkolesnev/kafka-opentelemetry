/*
 * Copyright 2022 Confluent Inc.
 */
package com.example.javaagent.instrumentation.helpers;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

import java.util.List;

/**
 * Span attribute processor that copies configured attributes from parent span (if one exist in
 * current application context).
 * <p>
 * Useful for attribute propagation through span chain - for example for spans to inherit service name
 * attribute from upstream local spans using this propagation.
 */
public class InheritedAttributesSpanProcessor implements SpanProcessor {

  private final List<AttributeKey<String>> inheritAttributeKeys;

  public InheritedAttributesSpanProcessor(List<AttributeKey<String>> inheritAttributeKeys) {
    this.inheritAttributeKeys = inheritAttributeKeys;
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    Span parentSpan = Span.fromContextOrNull(parentContext);
    if (parentSpan == null) {
      return;
    }
    if (!(parentSpan instanceof ReadableSpan)) {
      return;
    }
    ReadableSpan parentReadableSpan = (ReadableSpan) parentSpan;

    for (AttributeKey<String> inheritAttributeKey : inheritAttributeKeys) {
      String value = parentReadableSpan.getAttribute(inheritAttributeKey);
      if (value != null) {
        span.setAttribute(inheritAttributeKey, value);
      }
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
  }

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
