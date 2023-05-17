package com.github.rkolesnev.kafka.opentelemetry;

import com.github.rkolesnev.kafka.opentelemetry.Constants.Topics;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BaseStreamsStatelessAgent {

  private static final String BOOTSTRAP_SERVERS_ENV_VAR = "BOOTSTRAP_SERVERS";
  private static final String TOPIC_IN_ENV_VAR = "TOPIC_IN";
  private static final String TOPIC_OUT_ENV_VAR = "TOPIC_OUT";
  private static final String APPLICATION_ID_ENV_VAR = "APPLICATION_ID";

  private static final String DEFAULT_BOOTSTRAP_SERVERS = Constants.BOOTSTRAP_SERVERS;
  private static final String DEFAULT_TOPIC_IN = Topics.TopicA;
  private static final String DEFAULT_TOPIC_OUT = Topics.TopicB;
  private static final String DEFAULT_APPLICATION_ID = "streams-stateless-agent";

  private static final Logger log = LogManager.getLogger(BaseStreamsStatelessAgent.class);

  protected String bootstrapServers;
  protected String topicIn;
  protected String topicOut;
  protected String applicationId;
  protected KafkaStreams streams;

  public void run(Properties props, KafkaClientSupplier clientSupplier) {
    StreamsBuilder builder = new StreamsBuilder();

    builder.stream(this.topicIn, Consumed.with(Serdes.String(), Serdes.String()))
        .mapValues(v->v.toUpperCase()).to(this.topicOut);

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
    props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
    props.put(ProducerConfig.LINGER_MS_CONFIG,0);
    return props;
  }
}
