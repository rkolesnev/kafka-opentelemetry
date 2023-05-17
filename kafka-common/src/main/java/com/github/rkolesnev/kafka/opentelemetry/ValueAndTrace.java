package com.github.rkolesnev.kafka.opentelemetry;

public class ValueAndTrace<V> {
  private String traceparent;

  private V value;

  public ValueAndTrace(String traceparent, V value) {
    this.traceparent = traceparent;
    this.value = value;
  }

  public String getTraceparent() {
    return traceparent;
  }

  public void setTraceparent(String traceparent) {
    this.traceparent = traceparent;
  }


  public V getValue() {
    return value;
  }

  public void setValue(V value) {
    this.value = value;
  }
}
