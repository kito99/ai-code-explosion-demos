package com.example.duke;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/** Builds pooled database connections. */
public final class DataSources {

    private DataSources() {
    }

    public static HikariDataSource create(final String url, final String user, final String password) {
        final var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        return new HikariDataSource(config);
    }
}
