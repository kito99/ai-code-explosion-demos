package com.example.dukeguess.health;

import com.example.dukeguess.leaderboard.LeaderboardService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/// Reports the app ready once the leaderboard store answers a query.
@Readiness
@ApplicationScoped
public class LeaderboardReadinessCheck implements HealthCheck {

    private static final String CHECK_NAME = "leaderboard";

    @Inject
    private LeaderboardService leaderboardService;

    @Override
    public HealthCheckResponse call() {
        final var builder = HealthCheckResponse.named(CHECK_NAME);
        try {
            builder.withData("entries", leaderboardService.bestResults().size()).up();
        } catch (final RuntimeException exception) {
            builder.withData("error", String.valueOf(exception.getMessage())).down();
        }
        return builder.build();
    }
}
