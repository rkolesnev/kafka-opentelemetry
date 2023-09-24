/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.javaagent.instrumentation;

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;

import java.util.List;

import net.bytebuddy.matcher.ElementMatcher;

/**
 * This is a demo instrumentation which hooks into servlet invocation and modifies the http
 * response.
 */
@AutoService(InstrumentationModule.class)
public final class KStreamStateStoreInstrumentationModule extends InstrumentationModule {
    public KStreamStateStoreInstrumentationModule() {
        super("kafka-streams");
    }

    /*
    Instrumentation application order doesn't matter in this case as instrumented classes are different
    from javaagent built-in kafka streams instrumentation.
     */
    @Override
    public int order() {
        return 1;
    }

    @Override
    public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
        return AgentElementMatchers.hasClassesNamed("org.apache.kafka.streams.state.internals.WindowStoreBuilder");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return singletonList(new WindowStoreBuilderInstrumentation());
    }

    /**
     * @param className The name of the class that may or may not be a helper class.
     * @return whether the class is a helper class based on package
     */
    @Override
    public boolean isHelperClass(String className) {
        return className.startsWith("com.example.javaagent.instrumentation.helpers");
    }
}
