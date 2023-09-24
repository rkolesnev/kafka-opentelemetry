/*
 * Copyright 2022 Confluent Inc.
 */
package com.example.javaagent.instrumentation.helpers;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.StateStoreContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.apache.kafka.streams.state.internals.WrappedStateStore;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static com.example.javaagent.instrumentation.helpers.OtelUtils.*;

/**
 * Tracing WindowStore - delegates calls to wrapped {@link WindowStore} adding tracing behaviour
 * where appropriate.
 */
public class TracingWindowStore extends WrappedStateStore<WindowStore<Bytes, byte[]>, Bytes, byte[]> implements
        WindowStore<Bytes, byte[]> {

    public TracingWindowStore(WindowStore<Bytes, byte[]> wrapped) {
        super(wrapped);
    }

    @Override
    public void init(ProcessorContext context, StateStore root) {
        wrapped().init(context, root);
    }

    @Override
    public void init(StateStoreContext context, StateStore root) {
        wrapped().init(context, root);
    }

    @Override
    public void put(Bytes key, byte[] value, long windowStartTimestamp) {
        String spanName = wrapped().name() + " state-store-put";
        Span span = startSpan(spanName, Context.current(), null);
        try (Scope ignored = span.makeCurrent()) {
            wrapped().put(key, wrapValueWithTrace(value,
                    traceIdStringFromContext(Context.current()).getBytes(StandardCharsets.UTF_8)), windowStartTimestamp);
            span.end();
        }
    }

    @Override
    public byte[] fetch(Bytes key, long time) {
        String spanName = wrapped().name() + " state-store-get";

        Instant startTs = Instant.now();

        byte[] returned = wrapped().fetch(key, time);
        return createGetSpanAndReturnUnwrappedValue(returned, startTs, spanName);
    }

    @Override
    public WindowStoreIterator<byte[]> fetch(Bytes key, long timeFrom, long timeTo) {
        WindowStoreIterator<byte[]> resultIter = wrapped().fetch(key, timeFrom, timeTo);
        return new TracingWindowStoreIterator(resultIter, wrapped().name());
    }

    @Override
    public WindowStoreIterator<byte[]> backwardFetch(Bytes key, long timeFrom, long timeTo) {
        WindowStoreIterator<byte[]> resultIter = wrapped().backwardFetch(key, timeFrom, timeTo);
        return new TracingWindowStoreIterator(resultIter, wrapped().name());
    }

    @Override
    public KeyValueIterator<Windowed<Bytes>, byte[]> fetch(Bytes from, Bytes to, long timeFrom,
                                                           long timeTo) {
        KeyValueIterator<Windowed<Bytes>, byte[]> resultIter = wrapped().fetch(from, to, timeFrom,
                timeTo);
        return resultIter;
    }

    @Override
    public KeyValueIterator<Windowed<Bytes>, byte[]> backwardFetch(Bytes from, Bytes to, long timeFrom,
                                                                   long timeTo) {
        KeyValueIterator<Windowed<Bytes>, byte[]> resultIter = wrapped().backwardFetch(from, to, timeFrom,
                timeTo);
        return resultIter;
    }

    @Override
    public KeyValueIterator<Windowed<Bytes>, byte[]> all() {
        KeyValueIterator<Windowed<Bytes>, byte[]> resultIter = wrapped().all();
        return resultIter;
    }

    @Override
    public KeyValueIterator<Windowed<Bytes>, byte[]> backwardAll() {
        KeyValueIterator<Windowed<Bytes>, byte[]> resultIter = wrapped().backwardAll();
        return resultIter;
    }

    @Override
    public KeyValueIterator<Windowed<Bytes>, byte[]> fetchAll(long timeFrom, long timeTo) {
        KeyValueIterator<Windowed<Bytes>, byte[]> resultIter = wrapped().fetchAll(timeFrom, timeTo);
        return resultIter;
    }

    @Override
    public KeyValueIterator<Windowed<Bytes>, byte[]> backwardFetchAll(long timeFrom, long timeTo) {
        KeyValueIterator<Windowed<Bytes>, byte[]> resultIter = wrapped().backwardFetchAll(timeFrom, timeTo);
        return resultIter;
    }
}
