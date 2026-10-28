package com.example.duke.support;

import com.example.duke.game.SecretNumberGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/// Makes every game's secret 7 so API tests are deterministic.
@TestConfiguration(proxyBeanMethods = false)
public class FixedSecretConfiguration {

    public static final int SECRET = 7;

    @Bean
    @Primary
    SecretNumberGenerator fixedSecret() {
        return () -> SECRET;
    }
}
