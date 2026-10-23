package tech.virtua.duke;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.virtua.duke.HttpStatus.METHOD_NOT_ALLOWED;
import static tech.virtua.duke.HttpStatus.NOT_FOUND;
import static tech.virtua.duke.HttpStatus.OK;

/// Checks that the browser gets everything it needs to draw the game.
class FrontEndEndToEndTest {

    private static final int EPHEMERAL_PORT = 0;
    private static final int SECRET = 7;

    private GameServer server;
    private ApiClient browser;

    @BeforeEach
    void startServer() throws IOException {
        server = GameServer.start(EPHEMERAL_PORT, () -> SECRET);
        browser = new ApiClient(server.port());
    }

    @AfterEach
    void stopServer() {
        browser.close();
        server.close();
    }

    @ParameterizedTest
    @CsvSource({
            "/, text/html",
            "/index.html, text/html",
            "/styles.css, text/css",
            "/app.js, text/javascript",
            "/duke-thinking.svg, image/svg+xml",
            "/duke-wave.svg, image/svg+xml"})
    void servesEachAssetWithItsContentType(final String path, final String expectedContentType)
            throws Exception {
        final var response = browser.get(path);

        assertEquals(OK.code(), response.statusCode());
        assertTrue(response.contentType().startsWith(expectedContentType),
                () -> "%s was served as %s".formatted(path, response.contentType()));
        assertTrue(response.body().length() > 0, () -> path + " was empty");
    }

    @Test
    void servesAPageThatLoadsTheStyleAndTheScript() throws Exception {
        final var page = browser.get("/").body();

        assertTrue(page.contains("styles.css"));
        assertTrue(page.contains("app.js"));
        assertTrue(page.contains("duke-thinking.svg"));
    }

    @Test
    void servesTheOfficialDukeArtwork() throws Exception {
        final var waving = browser.get("/duke-wave.svg").body();

        assertTrue(waving.contains("<svg"));
        assertTrue(waving.contains("github.com/openjdk/duke"));
    }

    @Test
    void answersNotFoundForAnythingElse() throws Exception {
        assertEquals(NOT_FOUND.code(), browser.get("/secret-number.txt").statusCode());
        assertEquals(NOT_FOUND.code(), browser.get("/../pom.xml").statusCode());
    }

    @Test
    void refusesToBePostedTo() throws Exception {
        final var response = browser.post("/index.html");

        assertEquals(METHOD_NOT_ALLOWED.code(), response.statusCode());
        assertEquals("GET", response.allow());
    }
}
