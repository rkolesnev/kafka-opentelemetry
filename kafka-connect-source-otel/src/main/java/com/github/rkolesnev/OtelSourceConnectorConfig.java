package com.github.rkolesnev;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Width;

import java.util.Map;

public class OtelSourceConnectorConfig extends AbstractConfig {

  public final static String NUM_RECORDS = "num.records";
  private final static String NUM_RECORDS_DOC = "Number of records to emit in a poll";
  private final static String NUM_RECORDS_DISPLAY = "Number of records";
  public final static int NUM_RECORDS_DEFAULT = 2;

  public final static String TOPIC = "topic";
  private final static String TOPIC_DOC = "Topic to send records to";
  private final static String TOPIC_DISPLAY = "Topic";
  private static final String TOPIC_DEFAULT = "TopicA";

  public static final ConfigDef CONFIG_DEF = baseConfigDef();
  private static final String CONNECTOR_GROUP = "Connector";

  protected static ConfigDef baseConfigDef() {
    ConfigDef config = new ConfigDef();
    addConnectorOptions(config);
    return config;
  }

  private static void addConnectorOptions(ConfigDef config) {
    int orderInGroup = 0;
    config.define(
        NUM_RECORDS,
        Type.INT,
        NUM_RECORDS_DEFAULT,
        Importance.HIGH,
        NUM_RECORDS_DOC,
        CONNECTOR_GROUP,
        ++orderInGroup,
        Width.SHORT,
        NUM_RECORDS_DISPLAY
    ).define(
        TOPIC,
        Type.STRING,
        TOPIC_DEFAULT,
        Importance.HIGH,
        TOPIC_DOC,
        CONNECTOR_GROUP,
        ++orderInGroup,
        Width.SHORT,
        TOPIC_DISPLAY
    );
  }

  public OtelSourceConnectorConfig(Map<String, String> properties) {
    super(CONFIG_DEF, properties);
  }

  protected OtelSourceConnectorConfig(ConfigDef subclassConfigDef, Map<String, String> props) {
    super(subclassConfigDef, props);
  }

}
