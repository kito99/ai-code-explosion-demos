package com.example.guessduke;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/// Starts the database the tests run against, so the suite needs nothing running
/// beforehand beyond a Docker daemon. Spring reuses the context across test classes,
/// so one container serves them all.
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainerConfiguration {

    /// Matches the pinned image the app runs against in `compose.yaml`.
    private static final String POSTGRES_IMAGE = "postgres:18";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
