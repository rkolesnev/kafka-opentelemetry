/*
 * Copyright 2022 Confluent Inc.
 */
package com.example.javaagent.instrumentation.helpers;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.WindowStoreIterator;

import java.time.Instant;

import static com.example.javaagent.instrumentation.helpers.OtelUtils.createGetSpanAndReturnUnwrappedValue;

/**
 * Tracing WindowStoreIterator wrapper - used by {@link TracingWindowStore}
 * <p>
 * Implements {@link WindowStoreIterator} by delegating method calls to wrapped WindowStoreIterator
 * optionally executing tracing behaviour
 * <p>
 * Wraps store operations to execute tracing handling
 */
public class TracingWindowStoreIterator implements
        WindowStoreIterator<byte[]> {

    private final KeyValueIterator<Long, byte[]> wrapped;
    private final String storeName;

    public TracingWindowStoreIterator(KeyValueIterator<Long, byte[]> wrapped, String storeName) {
        this.storeName = storeName;
        this.wrapped = wrapped;
    }

    @Override
    public void close() {
        wrapped.close();
    }


    @Override
    public Long peekNextKey() {
        return wrapped.peekNextKey();
    }

    @Override
    public boolean hasNext() {
        return wrapped.hasNext();
    }

    @Override
    public KeyValue<Long, byte[]> next() {
        String spanName = storeName + " state-store-get";
        Instant startTs = Instant.now();
        KeyValue<Long, byte[]> keyValue = wrapped.next();
        byte[] bytesValue = keyValue.value;
        if (null == bytesValue) {
            return keyValue;
        }
        bytesValue = createGetSpanAndReturnUnwrappedValue(bytesValue, startTs, spanName);
        return new KeyValue<>(keyValue.key, bytesValue);
    }
}


