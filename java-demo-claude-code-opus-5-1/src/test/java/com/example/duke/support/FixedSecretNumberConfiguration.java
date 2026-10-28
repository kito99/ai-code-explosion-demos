package com.example.duke.support;

import com.example.duke.game.SecretNumberGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/// Makes Duke think of the same number every round, so API tests can play a known game.
@TestConfiguration(proxyBeanMethods = false)
public class FixedSecretNumberConfiguration {

    /// The number Duke picks in every test.
    public static final int SECRET_NUMBER = 7;

    @Bean
    @Primary
    public SecretNumberGenerator fixedSecretNumberGenerator() {
        return () -> SECRET_NUMBER;
    }
}
