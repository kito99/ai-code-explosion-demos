package com.example.dukeguess.support;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/// Adds a registry that keeps running totals.
///
/// The console registry the app ships with reports per-interval values, which say nothing
/// about what a single test just did, so assertions read from this one instead.
@TestConfiguration(proxyBeanMethods = false)
public class MetricsCaptureConfiguration {

    @Bean
    SimpleMeterRegistry metricsCaptureRegistry() {
        return new SimpleMeterRegistry();
    }
}
