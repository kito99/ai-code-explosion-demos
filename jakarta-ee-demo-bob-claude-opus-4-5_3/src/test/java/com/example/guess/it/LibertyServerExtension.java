package com.example.guess.it;

import org.junit.jupiter.api.extension.*;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * JUnit 5 extension for integration tests.
 *
 * <p>It starts a PostgreSQL Testcontainers container, writes the DB coordinates
 * into a Liberty bootstrap.properties file, then starts the Liberty server.
 * After all tests it stops the server and the container.
 *
 * <p>The server is expected to have been built (WAR + Liberty install-feature)
 * before tests run, which the liberty-maven-plugin does during
 * {@code pre-integration-test} without the {@code start} goal.
 */
public class LibertyServerExtension implements BeforeAllCallback, AfterAllCallback {

    private static PostgreSQLContainer<?> pg;

    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception {
        // Start database
        pg = new PostgreSQLContainer<>("postgres:18")
                .withDatabaseName("guessdb")
                .withUsername("guessuser")
                .withPassword("guesspass");
        pg.start();

        // Pass coordinates to the Liberty server via bootstrap.properties
        Path bootstrapProps = libertyBootstrapPropertiesPath();
        Files.createDirectories(bootstrapProps.getParent());
        String props = String.join("\n",
                "db.host=" + pg.getHost(),
                "db.port=" + pg.getMappedPort(5432),
                "db.name=" + pg.getDatabaseName(),
                "db.user=" + pg.getUsername(),
                "db.password=" + pg.getPassword(),
                "default.http.port=9080",
                "default.https.port=9443",
                "");
        Files.writeString(bootstrapProps, props);

        // Start Liberty server
        startLiberty();
    }

    @Override
    public void afterAll(ExtensionContext ctx) throws Exception {
        stopLiberty();
        if (pg != null) pg.stop();
    }

    // ---- Liberty lifecycle ----

    private static void startLiberty() throws IOException, InterruptedException {
        Process p = new ProcessBuilder(serverScript(), "start", "guessServer")
                .inheritIO()
                .start();
        if (!p.waitFor(120, TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException("Liberty server failed to start");
        }
    }

    private static void stopLiberty() throws IOException, InterruptedException {
        Process p = new ProcessBuilder(serverScript(), "stop", "guessServer")
                .inheritIO()
                .start();
        p.waitFor(30, TimeUnit.SECONDS);
    }

    private static String serverScript() {
        String wlpBin = System.getProperty("liberty.wlp.dir",
                "target/liberty/wlp") + "/bin/server";
        // Maven runs from module root; resolve relative to CWD
        return new File(wlpBin).getAbsolutePath();
    }

    /** Returns the path to bootstrap.properties inside the Liberty server config dir. */
    private static Path libertyBootstrapPropertiesPath() {
        String serverConfigDir = System.getProperty("liberty.server.config.dir",
                "target/liberty/wlp/usr/servers/guessServer");
        return Path.of(serverConfigDir, "bootstrap.properties");
    }
}
