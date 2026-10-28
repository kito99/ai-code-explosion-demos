package com.example.dukeguess.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Settings for the rounds the server holds in memory.
///
/// @param sessionLifetime how long an untouched round stays in play before it is swept
@ConfigurationProperties(prefix = "duke.game")
public record GameProperties(Duration sessionLifetime) {

    private static final Duration DEFAULT_SESSION_LIFETIME = Duration.ofHours(2);

    public GameProperties {
        sessionLifetime = sessionLifetime == null ? DEFAULT_SESSION_LIFETIME : sessionLifetime;
    }
}
