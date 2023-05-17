package com.github.rkolesnev.kafka.opentelemetry;

import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.internals.RocksDBTimestampedStore;
import org.apache.kafka.streams.state.internals.RocksDbKeyValueBytesStoreSupplier;

public class TracingTimestampedRocksDBWrappedStoreSupplier implements KeyValueBytesStoreSupplier{

    private final RocksDbKeyValueBytesStoreSupplier inner;
    public TracingTimestampedRocksDBWrappedStoreSupplier(RocksDbKeyValueBytesStoreSupplier inner) {
      this.inner = inner;
    }

    @Override
    public String name() {
      return "tracing-"+inner.name();
    }

    @Override
    public KeyValueStore<Bytes, byte[]> get() {
      return new TracingTimestampedRocksDBWrappedStore((RocksDBTimestampedStore) inner.get());
    }

    @Override
    public String metricsScope() {
      return "rocksdb";
    }


}
