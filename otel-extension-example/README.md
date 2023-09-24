# Extensions

## Introduction

This is an example of instrumentation extension module for Kafka Streams / kSQL - it wraps Windowed State Store creation with custom tracing logic to allow preservation of traces in stateful topologies and span processing customizers for kSQL service name extraction and cleanup.

> Note: It is just an example of implementation - it has only minimal implementation required for Stateful Join operation execution and not in any way production ready, fully implemented code.

The contents in this folder demonstrate how to create an extension for the OpenTelemetry Java instrumentation agent, with examples.

> Read both the source code and the Gradle build script, as they contain documentation that explains the purpose of all the major components.

## Build and add extensions

To build this extension project, run `./gradlew build`. You can find the resulting jar file in `build/libs/`.

To add the extension to the instrumentation agent:

1. Copy the jar file to a host that is running an application to which you've attached the OpenTelemetry Java instrumentation.
2. Modify the startup command to add the full path to the extension file. For example:

   ```bash
   java -javaagent:path/to/opentelemetry-javaagent.jar \
        -Dotel.javaagent.extensions=build/libs/opentelemetry-java-instrumentation-extension-demo-1.0-all.jar
        -jar myapp.jar
   ```

Note: to load multiple extensions, you can specify a comma-separated list of extension jars or directories (that
contain extension jars) for the `otel.javaagent.extensions` value.

## Embed extensions in the OpenTelemetry Agent

To simplify deployment, you can embed extensions into the OpenTelemetry Java Agent to produce a single jar file. With an integrated extension, you no longer need the `-Dotel.javaagent.extensions` command line option.

For more information, see the `extendedAgent` task in [build.gradle](build.gradle).

## Extensions examples

[ExampleAutoConfigurationCustomizerProvider]: src/java/com/example/javaagent/instrumentation/ExampleAutoConfigurationCustomizerProvider.java
[InheritedAttributesSpanProcessor]: src/java/com/example/javaagent/instrumentation/helpers/InheritedAttributesSpanProcessor.java
[ServiceNameFromThreadExtractorProcessor]: src/java/com/example/javaagent/instrumentation/helpers/ServiceNameFromThreadExtractorProcessor.java
[DelegatingSpanExporter]: src/java/com/example/javaagent/instrumentation/helpers/DelegatingSpanExporter.java
[KStreamStateStoreInstrumentationModule]: src/java/com/example/javaagent/instrumentation/KStreamStateStoreInstrumentationModule.java

- Custom `AutoConfigurationCustomizer`: [ExampleAutoConfigurationCustomizerProvider][ExampleAutoConfigurationCustomizerProvider]
- Custom `SpanProcessor`: [InheritedAttributesSpanProcessor][InheritedAttributesSpanProcessor]
- Custom `SpanProcessor`: [ServiceNameFromThreadExtractorProcessor][ServiceNameFromThreadExtractorProcessor]
- Custom `SpanExporter`: [DelegatingSpanExporter][DelegatingSpanExporter]
- Additional instrumentation: [KStreamStateStoreInstrumentationModule][KStreamStateStoreInstrumentationModule]


