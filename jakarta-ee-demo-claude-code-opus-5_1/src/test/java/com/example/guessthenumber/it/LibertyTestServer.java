package com.example.guessthenumber.it;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/// Runs the application the way a player would: a PostgreSQL container the tests provision
/// themselves, and the Liberty server the build installed under `target`, pointed at it.
final class LibertyTestServer {

    private static final String DATABASE_NAME = "guessdb";
    private static final String DATABASE_USER = "guess";
    private static final String DATABASE_PASSWORD = "guess";
    private static final int POSTGRES_PORT = 5432;

    private static final Path INSTALL_DIRECTORY = Path.of(System.getProperty("liberty.installDirectory"));
    private static final String SERVER_NAME = System.getProperty("liberty.serverName");
    private static final int HTTP_PORT = Integer.getInteger("app.http.port");
    private static final DockerImageName POSTGRES_IMAGE =
            DockerImageName.parse(System.getProperty("postgres.image"));

    private static final Duration SERVER_COMMAND_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration READINESS_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration READINESS_POLL_INTERVAL = Duration.ofMillis(500);

    private static PostgreSQLContainer<?> database;
    private static boolean serverRunning;

    private LibertyTestServer() {
    }

    static synchronized URI start() throws IOException, InterruptedException {
        if (database == null) {
            database = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withDatabaseName(DATABASE_NAME)
                    .withUsername(DATABASE_USER)
                    .withPassword(DATABASE_PASSWORD);
            database.start();
            Runtime.getRuntime().addShutdownHook(new Thread(LibertyTestServer::stopQuietly));
        }
        if (!serverRunning) {
            runServerCommand("start");
            serverRunning = true;
            awaitReady();
        }
        return baseUri();
    }

    /// Stops and starts the server while the database keeps running, so a test can show
    /// that leaderboard entries outlive the application process.
    static synchronized void restart() throws IOException, InterruptedException {
        stopServer();
        start();
    }

    static synchronized void stop() throws IOException, InterruptedException {
        stopServer();
        if (database != null) {
            database.stop();
            database = null;
        }
    }

    static URI baseUri() {
        return URI.create("http://localhost:%d".formatted(HTTP_PORT));
    }

    /// Everything the server has written to its console log, which is where the exported
    /// telemetry lands. Liberty starts a fresh console log on every start, so this only ever
    /// holds what the currently running server produced.
    static String consoleLog() throws IOException {
        return Files.readString(consoleLogPath());
    }

    private static Path consoleLogPath() {
        return INSTALL_DIRECTORY.resolve(Path.of("usr", "servers", SERVER_NAME, "logs", "console.log"));
    }

    private static void stopServer() throws IOException, InterruptedException {
        if (serverRunning) {
            runServerCommand("stop");
            serverRunning = false;
        }
    }

    private static void stopQuietly() {
        try {
            stopServer();
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (final IOException failure) {
            // The JVM is on its way out; a server left running is reported by the next build.
        }
    }

    private static void runServerCommand(final String command) throws IOException, InterruptedException {
        final var output = INSTALL_DIRECTORY.getParent().resolve("liberty-%s.log".formatted(command)).toFile();
        final var builder = new ProcessBuilder(
                INSTALL_DIRECTORY.resolve(Path.of("bin", "server")).toString(), command, SERVER_NAME)
                .redirectErrorStream(true)
                .redirectOutput(output);
        // The server JVM inherits these, which is how server.xml picks up the container's port.
        builder.environment().putAll(databaseEnvironment());
        final var process = builder.start();

        if (!process.waitFor(SERVER_COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Liberty did not answer '%s' within %s".formatted(
                    command, SERVER_COMMAND_TIMEOUT));
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Liberty '%s' failed with exit code %d. Output:%n%s".formatted(
                    command, process.exitValue(), Files.readString(output.toPath())));
        }
    }

    private static Map<String, String> databaseEnvironment() {
        return Map.of(
                "DB_HOST", database.getHost(),
                "DB_PORT", String.valueOf(database.getMappedPort(POSTGRES_PORT)),
                "DB_NAME", DATABASE_NAME,
                "DB_USER", DATABASE_USER,
                "DB_PASSWORD", DATABASE_PASSWORD);
    }

    private static void awaitReady() throws InterruptedException {
        final var readinessUri = baseUri().resolve("/health/ready");
        final var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        final var request = HttpRequest.newBuilder(readinessUri).timeout(Duration.ofSeconds(5)).GET().build();
        final var deadline = System.nanoTime() + READINESS_TIMEOUT.toNanos();

        while (System.nanoTime() < deadline) {
            try {
                if (http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return;
                }
            } catch (final IOException notUpYet) {
                // The server is still starting; try again until the deadline passes.
            }
            Thread.sleep(READINESS_POLL_INTERVAL.toMillis());
        }
        throw new IllegalStateException("The application was not ready at %s within %s"
                .formatted(readinessUri, READINESS_TIMEOUT));
    }
}
