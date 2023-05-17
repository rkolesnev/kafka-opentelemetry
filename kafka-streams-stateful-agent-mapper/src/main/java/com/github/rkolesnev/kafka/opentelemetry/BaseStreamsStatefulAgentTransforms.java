package com.github.rkolesnev.kafka.opentelemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BaseStreamsStatefulAgentTransforms {

  private static final String BOOTSTRAP_SERVERS_ENV_VAR = "BOOTSTRAP_SERVERS";
  private static final String TOPIC_IN_ENV_VAR = "TOPIC_IN";
  private static final String TOPIC_OUT_ENV_VAR = "TOPIC_OUT";
  private static final String APPLICATION_ID_ENV_VAR = "APPLICATION_ID";

  private static final String DEFAULT_BOOTSTRAP_SERVERS = Constants.BOOTSTRAP_SERVERS;
  private static final String DEFAULT_TOPIC_IN = "TopicA";
  private static final String DEFAULT_TOPIC_OUT = "TopicB";
  private static final String DEFAULT_APPLICATION_ID = "streams-mapper";

  private static final Logger log = LogManager.getLogger(BaseStreamsStatefulAgentTransforms.class);

  protected String bootstrapServers;
  protected String topicIn;
  protected String topicOut;
  protected String applicationId;
  protected KafkaStreams streams;

  public void run(Properties props, KafkaClientSupplier clientSupplier) {
    StreamsBuilder builder = new StreamsBuilder();

    ValueAndTraceSerde<Long> longValueAndTraceSerde = new ValueAndTraceSerde<>(Serdes.Long());

    builder.stream(this.topicIn, Consumed.with(Serdes.String(), Serdes.String()))
        .groupByKey()
        .aggregate(() -> new ValueAndTrace<>(null, 0L), (key, value, aggregate) -> {
          String currentTraceId = OtelUtils.traceIdStringFromContext(
              Context.current());
          String previousSpan = aggregate.getTraceparent();
          Span aggregateSpan = OtelUtils.startSpan("aggregate-process", Context.current(),
              previousSpan);
          ValueAndTrace<Long> result;
          try (Scope ignored = aggregateSpan.makeCurrent()) {
            result = new ValueAndTrace<>(currentTraceId,
                aggregate.getValue() + 1L);
          }finally {
            aggregateSpan.end();
          }
          return result;
        }, Materialized.with(Serdes.String(), longValueAndTraceSerde)).toStream()
        .transform(
            new TransformerSupplier<String, ValueAndTrace<Long>, KeyValue<String, Long>>() {
              @Override
              public Transformer<String, ValueAndTrace<Long>, KeyValue<String, Long>> get() {
                return new Transformer<String, ValueAndTrace<Long>, KeyValue<String, Long>>() {
                  private ProcessorContext context;

                  @Override
                  public void init(org.apache.kafka.streams.processor.ProcessorContext context) {
                    this.context = context;
                  }

                  @Override
                  public KeyValue<String, Long> transform(String key, ValueAndTrace<Long> value) {
                    String storedContextId = value.getTraceparent();
                    Context parentContext;

                    if (storedContextId != null) {
                      parentContext = OtelUtils.contextFromTraceIdString(value.getTraceparent());
                    } else {
                      parentContext = Context.current();
                    }
                    Span aggregateOutSpan = OtelUtils.startSpan("aggregate-out", parentContext, null);
                    try (Scope ignored = aggregateOutSpan.makeCurrent()) {
                      context.forward(key, value.getValue());
                    }
                    aggregateOutSpan.end();
                    return null;
                  }

                  @Override
                  public void close() {

                  }
                };
              }
            })
        .mapValues(String::valueOf)
        .to(this.topicOut);

    Topology topology = builder.build();
    log.info(topology.describe());

    if (clientSupplier == null) {
      this.streams = new KafkaStreams(topology, props);
    } else {
      this.streams = new KafkaStreams(topology, props, clientSupplier);
    }
    this.streams.cleanUp();
    this.streams.start();
  }

  public void stop() {
    this.streams.close();
  }

  public void loadConfiguration(Map<String, String> map) {
    this.bootstrapServers = map.getOrDefault(BOOTSTRAP_SERVERS_ENV_VAR, DEFAULT_BOOTSTRAP_SERVERS);
    this.topicIn = map.getOrDefault(TOPIC_IN_ENV_VAR, DEFAULT_TOPIC_IN);
    this.topicOut = map.getOrDefault(TOPIC_OUT_ENV_VAR, DEFAULT_TOPIC_OUT);
    this.applicationId = map.getOrDefault(APPLICATION_ID_ENV_VAR, DEFAULT_APPLICATION_ID);
  }

  public Properties loadKafkaStreamsProperties() {
    Properties props = new Properties();
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, this.applicationId);
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    //props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
    props.put(ProducerConfig.LINGER_MS_CONFIG,0);
    return props;
  }
}
