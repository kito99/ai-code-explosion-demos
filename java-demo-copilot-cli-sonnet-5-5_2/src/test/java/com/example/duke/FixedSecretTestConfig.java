package com.example.duke;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.example.duke.game.SecretNumberGenerator;

/** Lets tests choose the secret number of the next game. */
@TestConfiguration(proxyBeanMethods = false)
public class FixedSecretTestConfig {

    public static final int DEFAULT_SECRET = 7;

    public static final AtomicInteger NEXT_SECRET = new AtomicInteger(DEFAULT_SECRET);

    @Bean
    @Primary
    SecretNumberGenerator fixedSecretNumberGenerator() {
        return NEXT_SECRET::get;
    }
}
