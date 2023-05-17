package com.github.rkolesnev;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Width;

import java.util.Map;

public class OtelSinkConnectorConfig extends AbstractConfig {

  public static final ConfigDef CONFIG_DEF = baseConfigDef();

  protected static ConfigDef baseConfigDef() {
    ConfigDef config = new ConfigDef();
    return config;
  }

  public OtelSinkConnectorConfig(Map<String, String> properties) {
    super(CONFIG_DEF, properties);
  }

  protected OtelSinkConnectorConfig(ConfigDef subclassConfigDef, Map<String, String> props) {
    super(subclassConfigDef, props);
  }

}
