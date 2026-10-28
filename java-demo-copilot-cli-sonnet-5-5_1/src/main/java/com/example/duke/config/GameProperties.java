package com.example.duke.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/// Settings for in-memory games.
///
/// @param sessionTimeout how long an untouched game is kept before it is discarded
@ConfigurationProperties(prefix = "duke.game")
public record GameProperties(@DefaultValue("30m") Duration sessionTimeout) {
}
