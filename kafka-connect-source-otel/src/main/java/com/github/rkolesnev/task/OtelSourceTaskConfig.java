package com.github.rkolesnev.task;


import com.github.rkolesnev.OtelSourceConnectorConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.Map;

/**
 * Configuration options for a single ElasticSourceTask. These are processed after all
 * Connector-level configs have been parsed.
 */
public class OtelSourceTaskConfig extends OtelSourceConnectorConfig {

    static ConfigDef config = baseConfigDef();

    public OtelSourceTaskConfig(Map<String, String> props) {
        super(config, props);
    }
}
