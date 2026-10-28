package com.example.dukeguess.telemetry;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.stereotype.Component;

/// Runs a unit of game work inside its own observation, which produces one span and one
/// timer per call.
@Component
public class GameTelemetry {

    private final ObservationRegistry observationRegistry;

    public GameTelemetry(final ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    public <T> T observe(final TracedOperation operation, final Supplier<T> work) {
        return observe(operation, _ -> work.get());
    }

    /// Runs `work`, handing it the live observation so it can tag the span with the
    /// outcome it discovers.
    public <T> T observe(final TracedOperation operation, final Function<Observation, T> work) {
        final var observation = Observation.createNotStarted(operation.observationName(), observationRegistry);
        return observation.observe(() -> work.apply(observation));
    }

    /// Tags the current observation with a value from a fixed set.
    public static void tag(final Observation observation, final TelemetryTag tag, final Enum<?> value) {
        observation.lowCardinalityKeyValue(tag.key(), value.name().toLowerCase(Locale.ROOT));
    }
}
