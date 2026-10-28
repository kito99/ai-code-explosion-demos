package com.example.duke;

import java.util.Map;
import java.util.Optional;

/** Runtime settings read from environment variables, defaulting to the local compose setup. */
public record AppConfig(int port, String databaseUrl, String databaseUser, String databasePassword) {

    static final String PORT_VARIABLE = "PORT";
    static final String DATABASE_URL_VARIABLE = "DATABASE_URL";
    static final String DATABASE_USER_VARIABLE = "DATABASE_USER";
    static final String DATABASE_PASSWORD_VARIABLE = "DATABASE_PASSWORD";

    static final int DEFAULT_PORT = 8080;
    static final String DEFAULT_DATABASE_URL = "jdbc:postgresql://localhost:5432/duke";
    static final String DEFAULT_DATABASE_USER = "duke";
    static final String DEFAULT_DATABASE_PASSWORD = "duke";

    public static AppConfig fromEnvironment(final Map<String, String> environment) {
        return new AppConfig(
                Optional.ofNullable(environment.get(PORT_VARIABLE)).map(Integer::parseInt).orElse(DEFAULT_PORT),
                environment.getOrDefault(DATABASE_URL_VARIABLE, DEFAULT_DATABASE_URL),
                environment.getOrDefault(DATABASE_USER_VARIABLE, DEFAULT_DATABASE_USER),
                environment.getOrDefault(DATABASE_PASSWORD_VARIABLE, DEFAULT_DATABASE_PASSWORD));
    }
}
