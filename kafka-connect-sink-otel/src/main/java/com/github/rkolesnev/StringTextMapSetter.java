package com.github.rkolesnev;

import io.opentelemetry.context.propagation.TextMapSetter;

public class StringTextMapSetter implements TextMapSetter<String[]> {

  private static final StringTextMapSetter instance = new StringTextMapSetter();

  public static StringTextMapSetter getInstance() {
    return instance;
  }

  @Override
  public void set(String[] carrier, String key, String value) {
    if ("traceparent".equals(key)) {
      carrier[0] = value;
    }
  }
}

