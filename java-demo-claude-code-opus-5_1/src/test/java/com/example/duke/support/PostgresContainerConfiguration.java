package com.example.duke.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/// Starts the database the tests run against, so the suite needs nothing running beforehand.
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfiguration {

    private static final String POSTGRES_IMAGE = "postgres:18";

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
