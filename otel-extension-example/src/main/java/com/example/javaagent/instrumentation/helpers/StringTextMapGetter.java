package com.example.javaagent.instrumentation.helpers;

import io.opentelemetry.context.propagation.TextMapGetter;

import static java.util.Collections.singletonList;

public class StringTextMapGetter implements TextMapGetter<String> {

  private static final StringTextMapGetter instance = new StringTextMapGetter();

  public static StringTextMapGetter getInstance() {
    return instance;
  }

  @Override
  public Iterable<String> keys(String carrier) {
    return singletonList("traceparent");
  }

  @Override
  public String get(String carrier, String key) {
    if ("traceparent".equals(key)) {
      return carrier;
    } else {
      return null;
    }
  }
}
