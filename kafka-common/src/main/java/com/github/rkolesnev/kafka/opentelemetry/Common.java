package com.github.rkolesnev.kafka.opentelemetry;

import java.util.Properties;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class Common {

  public static Properties getProducerProperties() {
    Properties props = new Properties();
    props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, Constants.BOOTSTRAP_SERVERS);
    props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        StringSerializer.class.getName());
    return props;
  }

  public static Properties getConsumerProperties() {
    Properties props = new Properties();
    props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, Constants.BOOTSTRAP_SERVERS);
    props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class.getName());
    props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        StringDeserializer.class.getName());
    props.setProperty(CommonClientConfigs.GROUP_ID_CONFIG,
        "c" + RandomUtils.nextInt(0, 10));// + UUID.randomUUID());
    props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    return props;
  }
}
