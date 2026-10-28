package com.example.dukeguess.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// Supplies the clock the game and the leaderboard timestamp with, so tests can pin time.
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemUTC();
    }
}
