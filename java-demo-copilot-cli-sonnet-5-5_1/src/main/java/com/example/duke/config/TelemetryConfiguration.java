package com.example.duke.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/// Sends traces and metrics to the app's own log, so the game is observable with nothing else
/// running. Pointing either at a real collector is a configuration change: see the README.
@Configuration(proxyBeanMethods = false)
public class TelemetryConfiguration {

    @Bean
    @ConditionalOnProperty(name = "duke.telemetry.console-traces", matchIfMissing = true)
    public LoggingSpanExporter loggingSpanExporter() {
        return LoggingSpanExporter.create();
    }

    @Bean
    @ConditionalOnProperty(name = "duke.telemetry.console-metrics", matchIfMissing = true)
    public LoggingMeterRegistry loggingMeterRegistry(final TelemetryProperties properties) {
        return new LoggingMeterRegistry(stepOf(properties.consoleMetricsInterval()), Clock.SYSTEM);
    }

    private static LoggingRegistryConfig stepOf(final Duration step) {
        return new LoggingRegistryConfig() {

            @Override
            public String get(final String key) {
                return null;
            }

            @Override
            public Duration step() {
                return step;
            }
        };
    }
}
