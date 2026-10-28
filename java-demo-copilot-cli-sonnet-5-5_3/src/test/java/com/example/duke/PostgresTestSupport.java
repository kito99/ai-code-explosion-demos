package com.example.duke;

import com.zaxxer.hikari.HikariDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** One PostgreSQL container shared by every integration test in the JVM. */
final class PostgresTestSupport {

    private static final String POSTGRES_IMAGE = "postgres:18.0";
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

    static {
        POSTGRES.start();
    }

    private PostgresTestSupport() {
    }

    static HikariDataSource newDataSource() {
        return DataSources.create(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    static void clearLeaderboard(final HikariDataSource dataSource) throws java.sql.SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("TRUNCATE leaderboard_entry RESTART IDENTITY");
        }
    }
}
