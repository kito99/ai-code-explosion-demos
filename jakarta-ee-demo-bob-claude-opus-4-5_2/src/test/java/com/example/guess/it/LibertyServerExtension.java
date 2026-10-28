package com.example.guess.it;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JUnit 5 extension that:
 * 1. Starts a PostgreSQL Testcontainer.
 * 2. Starts the Liberty server via `mvn liberty:start`, passing the DB coordinates
 *    as OS environment variables so server.xml's `${env.*}` variables resolve.
 * 3. Waits until the Liberty HTTP endpoint is ready.
 * 4. Stops Liberty and the container after all tests.
 */
public class LibertyServerExtension implements BeforeAllCallback, AfterAllCallback,
        ExtensionContext.Store.CloseableResource {

    static final int LIBERTY_PORT = 9080;

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> DB =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("guessdb")
                    .withUsername("guessuser")
                    .withPassword("guesspass");

    private static boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (started) return;
        started = true;

        DB.start();

        var projectDir = findProjectDir();

        // Pass DB coordinates as OS environment variables.
        // Liberty's server.xml reads ${env.DB_HOST} etc. directly from OS env.
        var dbEnv = Map.of(
                "DB_HOST",     DB.getHost(),
                "DB_PORT",     String.valueOf(DB.getMappedPort(5432)),
                "DB_NAME",     DB.getDatabaseName(),
                "DB_USER",     DB.getUsername(),
                "DB_PASSWORD", DB.getPassword()
        );

        var pb = new ProcessBuilder(isWindows() ? "mvn.cmd" : "mvn",
                "liberty:start", "-DserverStartTimeout=120");
        pb.directory(projectDir);
        pb.environment().putAll(dbEnv);
        pb.redirectErrorStream(true);

        var proc = pb.start();
        // Forward Liberty startup output to stderr for CI diagnostics.
        new Thread(() -> {
            try (var reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))) {
                reader.lines().forEach(line -> System.err.println("[liberty] " + line));
            } catch (Exception ignored) {}
        }, "liberty-output-drain").start();

        proc.waitFor(120, TimeUnit.SECONDS);
        waitForLibertyReady(LIBERTY_PORT, 60);

        context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL).put("liberty", this);
    }

    @Override
    public void afterAll(ExtensionContext context) {}

    @Override
    public void close() throws Exception {
        if (!started) return;
        try {
            var projectDir = findProjectDir();
            var pb = new ProcessBuilder(isWindows() ? "mvn.cmd" : "mvn", "liberty:stop");
            pb.directory(projectDir);
            pb.redirectErrorStream(true);
            pb.start().waitFor(30, TimeUnit.SECONDS);
        } finally {
            if (DB.isRunning()) DB.stop();
        }
    }

    private static void waitForLibertyReady(int port, int timeoutSeconds) throws Exception {
        var deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                var conn = (HttpURLConnection) new URL("http://localhost:" + port + "/")
                        .openConnection();
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                if (conn.getResponseCode() < 500) return;
            } catch (Exception ignored) {}
            Thread.sleep(1000);
        }
        throw new RuntimeException("Liberty did not become ready within " + timeoutSeconds + "s");
    }

    private static File findProjectDir() {
        var dir = new File(System.getProperty("user.dir"));
        while (dir != null) {
            if (new File(dir, "pom.xml").exists()) return dir;
            dir = dir.getParentFile();
        }
        throw new IllegalStateException(
                "pom.xml not found in any ancestor of " + System.getProperty("user.dir"));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
