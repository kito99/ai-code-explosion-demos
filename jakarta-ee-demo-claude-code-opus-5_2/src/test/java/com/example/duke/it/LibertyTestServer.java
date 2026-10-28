package com.example.duke.it;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/// Drives the Open Liberty server the build already installed under `target/liberty`. The
/// tests start it themselves so they can point it at a database the test provisioned, which
/// is what lets `mvn verify` run on a machine with nothing but Docker.
final class LibertyTestServer implements AutoCloseable {

    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration READINESS_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(250);

    /// Caps a single readiness probe, so a server that accepts the connection and then stalls
    /// cannot hold the deadline check hostage.
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(5);

    private final Path installDirectory;
    private final String serverName;
    private final URI baseUri;
    private final Map<String, String> databaseSettings;
    private final HttpClient probe = HttpClient.newBuilder()
            .connectTimeout(PROBE_TIMEOUT)
            .build();

    LibertyTestServer(final Path installDirectory, final String serverName, final int httpPort,
                      final Map<String, String> databaseSettings) {
        this.installDirectory = installDirectory;
        this.serverName = serverName;
        this.baseUri = URI.create(format("http://localhost:%d/", httpPort));
        this.databaseSettings = Map.copyOf(databaseSettings);
    }

    URI baseUri() {
        return baseUri;
    }

    /// Starts the server and waits until the application answers. Any server left running by
    /// an earlier run is stopped first, so a crashed run does not block the next one.
    void start() throws IOException, InterruptedException {
        runServerCommand("stop", false);
        runServerCommand("start", true);
        awaitApplication();
    }

    void stop() throws IOException, InterruptedException {
        runServerCommand("stop", false);
    }

    void restart() throws IOException, InterruptedException {
        stop();
        start();
    }

    /// Everything the server has written to the console since it last started, which is where
    /// the exported telemetry lands.
    String consoleLog() throws IOException {
        final var log = installDirectory.resolve(Path.of("usr", "servers", serverName, "logs", "console.log"));
        return Files.exists(log) ? Files.readString(log, StandardCharsets.UTF_8) : "";
    }

    @Override
    public void close() throws IOException, InterruptedException {
        stop();
    }

    private void runServerCommand(final String command, final boolean failOnError)
            throws IOException, InterruptedException {
        final var builder = new ProcessBuilder(
                installDirectory.resolve(Path.of("bin", "server")).toString(), command, serverName)
                .directory(installDirectory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT);
        // The server picks these up as Liberty variables, which is how it finds the database
        // the test just started.
        builder.environment().putAll(databaseSettings);

        final var process = builder.start();
        if (!process.waitFor(STARTUP_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException(format("The server did not answer '%s' in time", command));
        }
        if (failOnError && process.exitValue() != 0) {
            throw new IllegalStateException(
                    format("'server %s' failed with exit code %d", command, process.exitValue()));
        }
    }

    private void awaitApplication() throws IOException, InterruptedException {
        final var request = HttpRequest.newBuilder(baseUri.resolve("api/leaderboard"))
                .header("Accept", "application/json")
                .timeout(PROBE_TIMEOUT)
                .GET()
                .build();

        final var deadline = Instant.now().plus(READINESS_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            try {
                if (probe.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                    return;
                }
            } catch (final IOException notUpYet) {
                // The port is not accepting connections yet, which is expected while it starts.
            }
            Thread.sleep(POLL_INTERVAL);
        }
        throw new IllegalStateException("The application never became ready:\n" + consoleLog());
    }
}
