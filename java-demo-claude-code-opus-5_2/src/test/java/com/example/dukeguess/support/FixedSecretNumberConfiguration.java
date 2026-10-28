package com.example.dukeguess.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/// Replaces the random draw with one the test controls.
@TestConfiguration(proxyBeanMethods = false)
public class FixedSecretNumberConfiguration {

    @Bean
    @Primary
    FixedSecretNumberPicker fixedSecretNumberPicker() {
        return new FixedSecretNumberPicker();
    }
}
