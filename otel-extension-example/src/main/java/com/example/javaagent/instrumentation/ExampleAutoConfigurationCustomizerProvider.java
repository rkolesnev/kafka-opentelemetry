/*
 * Copyright 2022 Confluent Inc.
 */
package com.example.javaagent.instrumentation;

import com.example.javaagent.instrumentation.helpers.Constants;
import com.example.javaagent.instrumentation.helpers.DelegatingSpanExporter;
import com.example.javaagent.instrumentation.helpers.InheritedAttributesSpanProcessor;
import com.example.javaagent.instrumentation.helpers.ServiceNameFromThreadExtractorProcessor;
import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

import java.util.Collections;
import java.util.List;

/**
 * Configuration customizer for OpenTelemetry autoconfiguration
 * <p>
 * Allows to customize span processing pipeline by configuring custom span processors, exporters
 * etc.
 */
@AutoService(AutoConfigurationCustomizerProvider.class)
public class ExampleAutoConfigurationCustomizerProvider implements
        AutoConfigurationCustomizerProvider {

    private final List<AttributeKey<String>> inheritAttributeKeys =
            Collections.singletonList(Constants.SERVICE_NAME_KEY);

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        autoConfiguration.addTracerProviderCustomizer(
                (sdkTracerProviderBuilder, configProperties) -> sdkTracerProviderBuilder.addSpanProcessor(
                        new InheritedAttributesSpanProcessor(inheritAttributeKeys)).addSpanProcessor(new ServiceNameFromThreadExtractorProcessor()));
        autoConfiguration.addSpanExporterCustomizer(
                (spanExporter, configProperties) -> new DelegatingSpanExporter(spanExporter));

    }
}

