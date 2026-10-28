package com.example.dukeguess.support;

import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

/// Starts the database the suite runs against, so tests need nothing running beforehand.
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfiguration {

    private static final String POSTGRES_IMAGE = "postgres:18";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
