package com.example.duke;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    RandomGenerator randomGenerator() {
        return new SecureRandom();
    }
}
