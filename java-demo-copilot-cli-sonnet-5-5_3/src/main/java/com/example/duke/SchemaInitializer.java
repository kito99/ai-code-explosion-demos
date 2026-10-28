package com.example.duke;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import javax.sql.DataSource;

/** Creates the database tables on startup when they do not exist yet. */
public final class SchemaInitializer {

    private static final String SCHEMA_RESOURCE = "/schema.sql";

    private SchemaInitializer() {
    }

    public static void initialize(final DataSource dataSource) {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var schema = SchemaInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            statement.execute(new String(schema.readAllBytes(), StandardCharsets.UTF_8));
        } catch (SQLException exception) {
            throw new PersistenceException("Could not create the database schema.", exception);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
