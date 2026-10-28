package com.example.guess.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * CDI producer for the OpenTelemetry {@link Meter}.
 *
 * Liberty's mpTelemetry feature registers an {@link OpenTelemetry} CDI bean
 * that is fully initialized before application beans are created. Injecting it
 * here is the MicroProfile-recommended pattern and avoids a dependency on the
 * global singleton.
 */
@ApplicationScoped
public class MeterProducer {

    @Inject
    private OpenTelemetry openTelemetry;

    @Produces
    @ApplicationScoped
    public Meter produceMeter() {
        return openTelemetry.getMeter("guess-the-number");
    }
}
