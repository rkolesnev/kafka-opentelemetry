/*
 * Copyright 2022 Confluent Inc.
 */
package com.example.javaagent.instrumentation;

import com.example.javaagent.instrumentation.helpers.TracingWindowStore;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.streams.state.internals.TimestampedWindowStoreBuilder;
import org.apache.kafka.streams.state.internals.WindowStoreBuilder;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Instrumentation for {@link WindowStoreBuilder} and {@link TimestampedWindowStoreBuilder}.
 * <p>
 * Intercepts WindowStore creation on return from
 * {@link WindowStoreBuilder#maybeWrapLogging} and wraps returned WindowStore with
 * {@link TracingWindowStore}
 */
public class WindowStoreBuilderInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.apache.kafka.streams.state.internals.TimestampedWindowStoreBuilder").or(
        named("org.apache.kafka.streams.state.internals.WindowStoreBuilder")).or(
        named("org.apache.kafka.streams.state.internals.TimeOrderedWindowStoreBuilder"));
  }

  /**
   * Defines methods to transform using Advice classes.
   * <p>
   * Note that Advice class names specified as String to avoid pre-mature class loading
   */
  @Override
  public void transform(TypeTransformer transformer) {

    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPrivate())
            .and(named("maybeWrapLogging")),
        WindowStoreBuilderInstrumentation.class.getName() + "$GetAdvice");
  }

  public static class GetAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(
        @Advice.Return(readOnly = false) WindowStore<Bytes, byte[]> stateStore) {
      stateStore = new TracingWindowStore(stateStore);
    }
  }
}
