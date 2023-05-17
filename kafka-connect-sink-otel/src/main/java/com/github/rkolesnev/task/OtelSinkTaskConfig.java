package com.github.rkolesnev.task;


import com.github.rkolesnev.OtelSinkConnectorConfig;
import org.apache.kafka.common.config.ConfigDef;

import java.util.Map;

/**
 * Configuration options for a single ElasticSourceTask. These are processed after all
 * Connector-level configs have been parsed.
 */
public class OtelSinkTaskConfig extends OtelSinkConnectorConfig {

    static ConfigDef config = baseConfigDef();

    public OtelSinkTaskConfig(Map<String, String> props) {
        super(config, props);
    }
}
