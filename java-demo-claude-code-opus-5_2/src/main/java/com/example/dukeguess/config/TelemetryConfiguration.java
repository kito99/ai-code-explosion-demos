package com.example.dukeguess.config;

import java.time.Duration;

import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// Sends telemetry to the app's own log by default, so the game is observable with no
/// collector running.
///
/// Both exporters sit alongside the OTLP ones Spring Boot configures from
/// `management.*` properties rather than replacing them, so pointing the app at a real
/// collector is a configuration change and nothing here has to move.
@Configuration(proxyBeanMethods = false)
public class TelemetryConfiguration {

    private static final Duration METRIC_LOG_INTERVAL = Duration.ofSeconds(30);

    @Bean
    @ConditionalOnProperty(name = "duke.telemetry.console-traces", matchIfMissing = true)
    SpanExporter consoleSpanExporter() {
        return LoggingSpanExporter.create();
    }

    @Bean
    @ConditionalOnProperty(name = "duke.telemetry.console-metrics", matchIfMissing = true)
    LoggingMeterRegistry consoleMeterRegistry() {
        final var config = new LoggingRegistryConfig() {

            @Override
            public @Nullable String get(final String key) {
                return null;
            }

            @Override
            public Duration step() {
                return METRIC_LOG_INTERVAL;
            }
        };
        return new LoggingMeterRegistry(config, io.micrometer.core.instrument.Clock.SYSTEM);
    }
}
