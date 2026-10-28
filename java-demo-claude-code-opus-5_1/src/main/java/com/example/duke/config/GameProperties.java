package com.example.duke.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/// How long an in-progress round is kept in memory, and how often abandoned rounds are swept up.
///
/// @param sessionTimeout age at which a round nobody returned to is dropped
/// @param cleanupInterval how often the sweep runs
@ConfigurationProperties(prefix = "duke.game")
public record GameProperties(@DefaultValue("30m") Duration sessionTimeout,
                             @DefaultValue("5m") Duration cleanupInterval) {
}
