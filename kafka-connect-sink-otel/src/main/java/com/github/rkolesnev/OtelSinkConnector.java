package com.github.rkolesnev;

import com.github.rkolesnev.task.OtelSinkTask;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OtelSinkConnector extends SinkConnector {

  private static Logger logger = LoggerFactory.getLogger(OtelSinkConnector.class);

  private OtelSinkConnectorConfig config;
  private Map<String, String> configProperties;

  @Override
  public String version() {
    return Version.VERSION;
  }

  @Override
  public void start(Map<String, String> props) {
    try {
      configProperties = props;
      config = new OtelSinkConnectorConfig(props);
    } catch (ConfigException e) {
      throw new ConnectException("Couldn't start OtelSinkConnector due to configuration "
          + "error", e);
    }
  }

  @Override
  public Class<? extends Task> taskClass() {
    return OtelSinkTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    List<Map<String, String>> taskConfigs = new ArrayList<>(1);
    Map<String, String> taskProps = new HashMap<>(configProperties);
    taskConfigs.add(taskProps);
    return taskConfigs;
  }


  @Override
  public void stop() {
    logger.info("stopping otel sink");
  }

  @Override
  public ConfigDef config() {
    return OtelSinkConnectorConfig.CONFIG_DEF;
  }
}
