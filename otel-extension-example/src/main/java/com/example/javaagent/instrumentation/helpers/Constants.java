package com.example.javaagent.instrumentation.helpers;

import io.opentelemetry.api.common.AttributeKey;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

public class Constants {
    public static final AttributeKey<String> SERVICE_NAME_KEY = stringKey("service.name");

    public static final AttributeKey<String> THREAD_NAME_KEY = stringKey("thread.name");
    public static final AttributeKey<Boolean> TRANSIENT_QUERY_KEY = booleanKey("ksql.transient.query");
}
