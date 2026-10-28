package com.example.duke;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Enables @Observed AOP support for method-level tracing.
 * @Order(1) ensures the observation span wraps the transaction.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    @Order(1)
    ObservedAspect observedAspect(ObservationRegistry registry) {
        return new ObservedAspect(registry);
    }
}
