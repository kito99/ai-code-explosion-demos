package com.example.dukeguess;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Clock;

/// Makes the system clock injectable so timestamps can be fixed in tests.
@ApplicationScoped
public class ClockProducer {

    @Produces
    @ApplicationScoped
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
