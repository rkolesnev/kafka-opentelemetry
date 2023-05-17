package com.github.rkolesnev.kafka.opentelemetry;

import static com.github.rkolesnev.kafka.opentelemetry.OtelUtils.startSpan;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.StateStoreContext;
import org.apache.kafka.streams.query.Position;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.TimestampedBytesStore;
import org.apache.kafka.streams.state.internals.BatchWritingStore;
import org.apache.kafka.streams.state.internals.RocksDBTimestampedStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;

public class TracingTimestampedRocksDBWrappedStore implements
    KeyValueStore<Bytes, byte[]>, BatchWritingStore, TimestampedBytesStore {

  private static final Logger log = LogManager.getLogger(
      TracingTimestampedRocksDBWrappedStore.class);
  RocksDBTimestampedStore inner;

  public TracingTimestampedRocksDBWrappedStore(RocksDBTimestampedStore inner) {
    this.inner = inner;
  }

  private byte[] wrapValueWithTrace(byte[] value, byte[] trace) {
    if (value == null) {
      return null;
    }
    byte[] combined = Arrays.copyOf(value, value.length + trace.length);
    System.arraycopy(trace, 0, combined, value.length, trace.length);
    return combined;
  }

  private Pair<byte[], byte[]> unWrapValueWithTrace(byte[] valueAndTrace) {
    if (valueAndTrace == null) {
      return null;
    }
    if (valueAndTrace.length < 56) {
      return Pair.of(valueAndTrace, null);
    }
    int traceLength = 55;
    byte[] payload = new byte[valueAndTrace.length - traceLength];
    System.arraycopy(valueAndTrace, 0, payload, 0, payload.length);
    byte[] trace = new byte[traceLength];
    System.arraycopy(valueAndTrace, payload.length, trace, 0, trace.length);
    return Pair.of(payload, trace);
  }

  @Override
  public String name() {
    return "tracing-" + inner.name();
  }

  @Override
  public void init(ProcessorContext context, StateStore root) {
    inner.init(context, root);
  }

  @Override
  public void init(StateStoreContext context, StateStore root) {
    inner.init(context, root);
  }

  @Override
  public void flush() {
    inner.flush();
  }

  @Override
  public void close() {
    inner.close();
  }

  @Override
  public boolean persistent() {
    return inner.persistent();
  }

  @Override
  public boolean isOpen() {
    return inner.isOpen();
  }


  @Override
  public byte[] get(Bytes key) {
    log.info("state store get, store name: {}", name());

    String spanName = inner.name() + " state-store-get";
    if (Span.current().equals(Span.getInvalid())) {
      log.info("No valid current span");
    }
    Instant startTs = Instant.now();
    byte[] returned;

    returned = inner.get(key);
    if (returned == null) {
      Span span = startSpan(spanName, Context.current(), null, startTs);
      Scope ignored = span.makeCurrent();
      span.end();
      ignored.close();
      return null;

    } else {
      Pair<byte[], byte[]> valueAndTrace = unWrapValueWithTrace(returned);
      String storedSpanContext = null;
      if (valueAndTrace.getRight() == null) {
        log.warn("No trace data in store, key {}, state store get, store name: {}", key, name());
      } else {
        storedSpanContext = new String(valueAndTrace.getRight(), StandardCharsets.UTF_8);

      }
      Span span = startSpan(spanName, Context.current(), storedSpanContext, startTs);
      Scope ignored = span.makeCurrent();
      span.end();
      ignored.close();
      return valueAndTrace.getLeft();
    }
  }

  @Override
  public KeyValueIterator<Bytes, byte[]> range(Bytes from, Bytes to) {
    return inner.range(from, to);
  }

  @Override
  public KeyValueIterator<Bytes, byte[]> all() {
    return inner.all();
  }

  @Override
  public long approximateNumEntries() {
    return inner.approximateNumEntries();
  }

  @Override
  public void put(Bytes key, byte[] value) {
    log.info("state store put, store name: {}", name());
    String spanName = inner.name() + " state-store-put";
    Span currentSpan = Span.current();
    Span span = startSpan(spanName, Context.current(), null);
    try (Scope ignored = span.makeCurrent()) {
      inner.put(key, wrapValueWithTrace(value,
          OtelUtils.traceIdStringFromContext(Context.current()).getBytes(StandardCharsets.UTF_8)));
      span.end();
    }
    currentSpan.makeCurrent();
  }

  @Override
  public byte[] putIfAbsent(Bytes key, byte[] value) {
    log.info("state store putIfAbsent, store name: {}", name());

    String spanName = inner.name() + " state-store-put";
    Span currentSpan = Span.current();
    Span span = startSpan(spanName, Context.current(), null);
    byte[] returned;

    try (Scope ignored = span.makeCurrent()) {
      returned = inner.putIfAbsent(key, wrapValueWithTrace(value,
          OtelUtils.traceIdStringFromContext(Context.current()).getBytes(StandardCharsets.UTF_8)));
      span.end();
    }
    currentSpan.makeCurrent();

    if (returned == null) {
      return null;
    }
    return unWrapValueWithTrace(returned).getLeft();
  }

  @Override
  public void putAll(List<KeyValue<Bytes, byte[]>> entries) {
    log.info("state store putAll, store name: {}", name());
    inner.putAll(entries);
  }

  @Override
  public byte[] delete(Bytes key) {
    log.info("state store delete, store name: {}", name());

    return new byte[0];
  }

  @Override
  public void addToBatch(KeyValue<byte[], byte[]> record, WriteBatch batch)
      throws RocksDBException {
    log.info("state store addToBatch, store name: {}", name());
    inner.addToBatch(record, batch);
  }

  @Override
  public void write(WriteBatch batch) throws RocksDBException {
    log.info("state store write, store name: {}", name());
    inner.write(batch);
  }

  @Override
  public Position getPosition() {
    return inner.getPosition();
  }
}
