/*
 * Copyright 2022 Confluent Inc.
 */
package com.example.javaagent.instrumentation.helpers;

import com.example.javaagent.instrumentation.ExampleAutoConfigurationCustomizerProvider;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.example.javaagent.instrumentation.helpers.Constants.SERVICE_NAME_KEY;

/**
 * Enables overwriting Resource attributes when span is exported. Generally Resource attributes are
 * set at Span creation in specific restricted way and are read-only - so this approach has to be
 * used when there is need to set Resource attribute value more flexibly.
 * <p>
 * Configured by {@link ExampleAutoConfigurationCustomizerProvider}
 */
public class DelegatingSpanExporter implements SpanExporter {

  private final SpanExporter delegate;

  public DelegatingSpanExporter(SpanExporter delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    return delegate.export(
        spans.stream().map(this::overrideServiceName).collect(Collectors.toList()));
  }

  private SpanData overrideServiceName(SpanData spanData) {
    return new ServiceNameOverridingSpanData(spanData);
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  @Override
  public void close() {
    delegate.close();
  }


  static class ServiceNameOverridingSpanData extends DelegatingSpanData {

    private final String serviceName;

    protected ServiceNameOverridingSpanData(SpanData delegate) {
      super(delegate);
      this.serviceName = delegate.getAttributes().get(SERVICE_NAME_KEY);
    }

    @Override
    public Resource getResource() {
      Resource resource = super.getResource();
      if (serviceName != null && serviceName.length() > 0) {
        resource = resource.toBuilder().put(SERVICE_NAME_KEY, serviceName).build();
      }
      return resource;
    }

    @Override
    public Attributes getAttributes() {
      return super.getAttributes().toBuilder().remove(SERVICE_NAME_KEY).build();
    }
  }
}

