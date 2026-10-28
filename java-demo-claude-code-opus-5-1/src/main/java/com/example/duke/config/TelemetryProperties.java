package com.example.duke.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/// Console telemetry settings.
///
/// Whether the console exporters are registered at all is decided by
/// `duke.telemetry.console-traces` and `duke.telemetry.console-metrics`, which switch the
/// exporter beans on and off and so are read before any of this is bound.
///
/// @param consoleMetricsInterval how often the metric snapshot is written to the log
@ConfigurationProperties(prefix = "duke.telemetry")
public record TelemetryProperties(@DefaultValue("30s") Duration consoleMetricsInterval) {
}
