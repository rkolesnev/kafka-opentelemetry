## KSQL docker based example
### Running
- Download opentelemetry java agent from [Opentelemetry JavaAgent Releases](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases)

- Update docker_compose to use that version of javaagent - at this time latest is 1.30.0 - update to the version downloaded:
``` 
 ksqldb-server:
 ...
    volumes:
       - ./opentelemetry-javaagent-1.30.0.jar:/usr/share/java/otel/opentelemetry-javaagent.jar
```
- Build the extension in `otel-extension-example` and place in the `ksql-example` folder keeping name of `opentelemetry-java-instrumentation-extension-demo-1.0-all.jar`
- Run `docker-compose up -d`
- Once the docker initialization is complete - you can start using the created cluster and explore traces by accessing:
  - Jaeger at http://127.0.0.1:16686/
  - Control Center at http://localhost:9021/
  - produce data using Kafka bootstrap servers at localhost:9092
  - Use sample KSQL queries from KSQL.txt 
- To reset state run `docker-compose down -v` and then `docker-compose up -d` again.