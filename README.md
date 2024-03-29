# Instrumenting Kafka clients with OpenTelemetry

Example kafka applications with OpenTelemetry instrumentation

otel-extension-example - Example java agent extension for KStreams and kSQL state store operation tracing

ksql-example - docker compose environment and kSQL scripts for running ksql demo with java agent and extension from otel-extension-example module

kafka-consumer-auto - Kafka Consumer application with OpenTelemetry configured using AutoConfiguration

kafka-producer-auto - Kafka Producer application with OpenTelemetry configured using AutoConfiguration

kafka-producer - Kafka Producer application with OpenTelemetry configured using explicit configuration in code.

kafka-consumer - Kafka Consumer application with OpenTelemetry configured using explicit configuration in code.

kafka-producer-agent - Kafka Producer application with OpenTelemetry configured using Javaagent auto-instrumentation 

kafka-consumer-agent - Kafka Consumer application with OpenTelemetry configured using Javaagent auto-instrumentation.

kafka-producer-wrap - Kafka Producer application with OpenTelemetry wrapper

kafka-consumer-wrap - Kafka Consumer application with OpenTelemetry wrapper

kafka-streams-stateful-agent-wrapped-statestore - Kafka Streams stateful application with Javaagent and Tracing state store wrapper.

kafka-streams-stateful-agent-mapper - Kafka Streams stateful application with Javaagent and Tracing transformers.

kafka-streams-stateless-wrap - Kafka Streams stateless application with OpenTelemetry wrapped Kafka clients injected.

kafka-streams-stateless-agent - Kafka Streams stateless application with Javaagent.

kafka-streams-stateful-wrap - Kafka Streams stateful application with OpenTelemetry wrapped Kafka clients injected.

kafka-consumer-producer - Kafka Consumer + Producer application with OpenTelemetry configured explicitly in code and use Tracing wrappers.

kafka-consumer-producer-agent - Kafka Consumer + Producer application with OpenTelemetry configured using Javaagent. 

kafka-consumer-producer-auto - Kafka Consumer + Producer application with OpenTelemetry configured using AutoConfiguration

kafka-connect-source-otel - Test source connector with Opentelemtry - uses AutoConfiguration and ExtendedTracingProducerInterceptor.
 
kafka-connect-sink-otel - Test sing connector with Opentelemetry - uses AutoConfiguration and TracingConsumerInteceptor.

## Running the example applications using supplied docker images 

- download Javaagent instrumentation jar by running download-javaagent.sh
- start docker set by running docker-compose up -d in the docker subfolder
- For connectors - first build the connector jars and then copy them into docker/connect-jars subfolder prior to starting docker images.
- 
In order to get tracing information out of your application using Kafka clients, there are two ways to do so:

* instrumenting your application by enabling the tracing on the Kafka clients;
* using an external agent running alongside your application to add tracing;

## Instrumenting the Kafka clients based application

Instrumenting the application means enabling the tracing in the Kafka clients.
First, you need to add the dependency to the Kafka clients instrumentation library.

```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-kafka-clients-2.6</artifactId>
</dependency>
```

Also, depending on the exporter that you want to use for exporting tracing information, you have to add the corresponding dependency as well.
For example, in order to use the Jaeger exporter, the dependency is the following one.

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-jaeger</artifactId>
</dependency>
```

### Setting up the OpenTelemetry instance

In order to enable tracing on the Kafka clients, it is needed to create and register an `OpenTelemetry` instance globally.
This can be done in two different ways:

* using the SDK extension for environment-based autoconfiguration;
* using SDK builders for programmatic configuration;

#### SDK extension: autoconfiguration

It is possible to configure a global `OpenTelemetry` instance by using environment variables thanks to the SDK extension for autoconfiguration, enabled with the following dependency.

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk-extension-autoconfigure</artifactId>
</dependency>
```

The main environment variables to be set are the following:

* `OTEL_SERVICE_NAME`: specify the logical service name;
* `OTEL_TRACES_EXPORTER`: the list of exporters to be used for tracing. For example, by using `jaeger` you also need to have the corresponding dependency in the application;
* `OTEL_METRICS_EXPORTER`: the list of exporters to be used for metrics. If you don't use metrics then it has to be set to `none`;

Instead of using the above environment variables, it is also possible to use corresponding system properties to be set programmatically or on the command line.
They are `otel.service.name`, `otel.traces.exporter` and `otel.metrics.exporter`.

#### SDK builders: programmatic configuration

In order to build your own `OpenTelemetry` instance and not relying on autoconfiguration, it is possible to do so by using the SDK builders programmatically.
The OpenTelemetry SDK dependency is needed in order to have such SDK builders classes available.

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
</dependency>
```

The following code snippet sets the main attributes like the service name,then it configures the Jaeger exporter. 
Finally, it creates the `OpenTelemetry` instance and registers it globally so that it can be used by the Kafka clients.

```java
Resource resource = Resource.getDefault()
        .merge(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "my-kafka-service")));

SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(JaegerGrpcSpanExporter.builder().build()).build())
        .setSampler(Sampler.alwaysOn())
        .setResource(resource)
        .build();

OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .buildAndRegisterGlobal();
```

### Using interceptors

The Kafka clients API provides a way to "intercept" messages before they are sent to the brokers as well as messages received from the broker before being passed to the application.
The OpenTelemetry instrumented Kafka library provides two interceptors to be configured to add tracing information automatically.
The interceptor class has to be set in the properties bag used to create the Kafka client.

Use the `TracingProducerInterceptor` for the producer in order to create a "send" span automatically, each time a message is sent.

```java
props.setProperty(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());
```

Use the `TracingConsumerInterceptor` for the consumer in order to create a "receive" span automatically, each time a message is received.

```java
props.setProperty(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());
```

For a Streams API based application, you have to set the interceptors for the underlying producer and consumer.

```java
props.put(StreamsConfig.CONSUMER_PREFIX + ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());
props.put(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());
```

### Wrapping clients

The other way is by wrapping the Kafka client with a tracing enabled Kafka client.

Assuming you have a `Producer<K, V> producer` instance, you can wrap it in the following way.

```java
KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
Producer<String, String> tracingProducer = telemetry.wrap(producer);
```

Then use the `tracingProducer` as usual for sending messages to the Kafka cluster.

Assuming you have a `Consumer<K, V> consumer` instance, you can wrap it in the following way.

```java
KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
Consumer<String, String> tracingConsumer = telemetry.wrap(this.consumer);
```

Then use the `tracingConsumer` as usual for receiving messages from the Kafka cluster.

For a Streams API based application, you have to wrap the underlying producer and consumer.
This can be done by implementing the `KafkaClientSupplier` interface which returns the instances of producer and consumer used by the Streams API.
Or you can leverage the Kafka provided default implementation `DefaultKafkaClientSupplier`, to avoid code duplication, and wrapping producer and consumer to add the telemetry logic.

```java
private static class TracingKafkaClientSupplier extends DefaultKafkaClientSupplier {
    @Override
    public Producer<byte[], byte[]> getProducer(Map<String, Object> config) {
        KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
        return telemetry.wrap(super.getProducer(config));
    }

    @Override
    public Consumer<byte[], byte[]> getConsumer(Map<String, Object> config) {
        KafkaTelemetry telemetry = KafkaTelemetry.create(GlobalOpenTelemetry.get());
        return telemetry.wrap(super.getConsumer(config));
    }

    @Override
    public Consumer<byte[], byte[]> getRestoreConsumer(Map<String, Object> config) {
        return this.getConsumer(config);
    }

    @Override
    public Consumer<byte[], byte[]> getGlobalConsumer(Map<String, Object> config) {
        return this.getConsumer(config);
    }
}
```

## Using agent

Another way is by adding tracing to your application with no changes or additions into your application code.
You also don't need to add any dependencies to OpenTelemetry specific libraries.
It is possible by using the OpenTelemetry agent you can download from [here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases).
This agent has to run alongside your application in order to inject the logic for tracing messages sent and received to/from a Kafka cluster.

Run the producer application in the following way.

```shell
java -javaagent:path/to/opentelemetry-javaagent.jar \
      -Dotel.service.name=my-kafka-service \
      -Dotel.traces.exporter=jaeger \
      -Dotel.metrics.exporter=none \
      -jar kafka-producer-agent/target/kafka-producer-agent-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Run the consumer application similarly.

```shell
java -javaagent:path/to/opentelemetry-javaagent.jar \
      -Dotel.service.name=my-kafka-service \
      -Dotel.traces.exporter=jaeger \
      -Dotel.metrics.exporter=none \
      -Dotel.instrumentation.messaging.experimental.receive-telemetry.enabled=false \
      -jar kafka-consumer-agent/target/kafka-consumer-agent-1.0-SNAPSHOT-jar-with-dependencies.jar
```

As usual, the main three system properties are set to specify the logical service name, the exporter to be used (i.e. jaeger) and disable the metrics exporter.

The same can be used for running the Streams API based application.

The repo and readme is based on the https://github.com/ppatierno/kafka-opentelemetry repository / blog post.