package com.github.rkolesnev.kafka.opentelemetry;

import static java.util.Collections.singletonList;

import io.opentelemetry.context.propagation.TextMapGetter;

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
