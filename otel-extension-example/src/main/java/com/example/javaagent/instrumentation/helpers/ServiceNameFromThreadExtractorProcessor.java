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

import static com.example.javaagent.instrumentation.helpers.Constants.SERVICE_NAME_KEY;

/**
 * Span attribute processor that extracts service name from thread name attribute - for kSQL job name cleanup.
 * <p>
 * Checks if ServiceName attribute is already set - through attribute propagating processor and
 * if it iss not set - extracts it from Thread name following kSQL naming pattern.
 */
public class ServiceNameFromThreadExtractorProcessor implements SpanProcessor {

    public ServiceNameFromThreadExtractorProcessor() {
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        String serviceName = span.getAttribute(SERVICE_NAME_KEY);
        if (serviceName != null && serviceName.length() > 0) {
            return;
        }
        String threadName = span.getAttribute(Constants.THREAD_NAME_KEY);
        if (threadName == null || threadName.length() == 0) {
            return;
        }
        if(!isThreadAKsqlJob(threadName)){
            return;
        }
        if (isTransientQuery(threadName)) {
            span.setAttribute(Constants.TRANSIENT_QUERY_KEY, true);
            serviceName = extractTransientServiceName(threadName);
        } else {
            span.setAttribute(Constants.TRANSIENT_QUERY_KEY, false);
            serviceName = extractQueryServiceName(threadName);
        }
        span.setAttribute(SERVICE_NAME_KEY, serviceName);
    }

    private boolean isThreadAKsqlJob(String threadName) {
        return threadName.contains("ksql-clustertransient_") || threadName.contains("ksql-clusterquery_");
    }

    private boolean isTransientQuery(String threadName) {
        return threadName.contains("ksql-clustertransient_");
    }

    private String extractQueryServiceName(String threadName) {
        int startIndex = threadName.indexOf("ksql-clusterquery_") + "ksql-clusterquery_".length();
        int endIndex = threadName.indexOf("-", startIndex);
        return threadName.substring(startIndex, endIndex);
    }

    private String extractTransientServiceName(String threadName) {
        int startIndex = threadName.indexOf("ksql-clustertransient_") + "ksql-clustertransient_".length();
        int endIndex = threadName.indexOf("-", startIndex);
        return threadName.substring(startIndex, endIndex);
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
