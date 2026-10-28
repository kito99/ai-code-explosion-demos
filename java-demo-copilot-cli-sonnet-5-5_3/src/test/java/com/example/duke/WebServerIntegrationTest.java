package com.example.duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebServerIntegrationTest {

    private static final int SECRET = 7;
    private static final int OK = 200;
    private static final int CREATED = 201;
    private static final int BAD_REQUEST = 400;
    private static final int NOT_FOUND = 404;
    private static final int CONFLICT = 409;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private HikariDataSource dataSource;
    private WebServer server;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = PostgresTestSupport.newDataSource();
        SchemaInitializer.initialize(dataSource);
        PostgresTestSupport.clearLeaderboard(dataSource);
        server = startServer();
    }

    @AfterEach
    void tearDown() {
        server.stop();
        dataSource.close();
    }

    @Test
    void servesGamePageAndLeaderboardPage() throws Exception {
        assertTrue(get("/").body().contains("Guess the Number with Duke"));
        assertTrue(get("/leaderboard").body().contains("Leaderboard"));
        assertEquals(OK, get("/images/duke-hips.png").statusCode());
    }

    @Test
    void emptyLeaderboardHasNoEntries() throws Exception {
        assertEquals(0, json(get("/api/leaderboard")).get("entries").size());
    }

    @Test
    void playsToAWinAndPostsAScoreThatSurvivesARestart() throws Exception {
        final var gameId = newGameId();
        final var low = json(post("/api/games/" + gameId + "/guesses", Map.of("guess", "2")));
        assertEquals("TOO_LOW", low.get("result").asText());
        assertEquals(1, low.get("guessesMade").asInt());
        assertEquals(4, low.get("guessesRemaining").asInt());
        assertFalse(low.has("secretNumber"));

        final var win = json(post("/api/games/" + gameId + "/guesses", Map.of("guess", "7")));
        assertEquals("WON", win.get("status").asText());
        assertTrue(win.get("message").asText().contains("2 guesses"));
        assertEquals(CONFLICT, post("/api/games/" + gameId + "/guesses", Map.of("guess", "7")).statusCode());

        assertEquals(CREATED, post("/api/games/" + gameId + "/score", Map.of("name", "  Duke ")).statusCode());
        assertEquals(CONFLICT, post("/api/games/" + gameId + "/score", Map.of("name", "Duke")).statusCode());

        server.stop();
        server = startServer();
        final var entries = json(get("/api/leaderboard")).get("entries");
        assertEquals(1, entries.size());
        assertEquals("Duke", entries.get(0).get("name").asText());
        assertEquals(2, entries.get(0).get("guesses").asInt());
        assertEquals(1, entries.get(0).get("rank").asInt());
    }

    @Test
    void losingRevealsSecretAndCannotPostScore() throws Exception {
        final var gameId = newGameId();
        JsonNode last = null;
        for (var attempt = 0; attempt < GameRules.MAX_GUESSES; attempt++) {
            last = json(post("/api/games/" + gameId + "/guesses", Map.of("guess", "1")));
        }
        assertEquals("LOST", last.get("status").asText());
        assertEquals(SECRET, last.get("secretNumber").asInt());
        assertEquals(CONFLICT, post("/api/games/" + gameId + "/score", Map.of("name", "Duke")).statusCode());
        assertEquals(0, json(get("/api/leaderboard")).get("entries").size());
    }

    @Test
    void invalidInputGivesFriendlyErrorsAndChangesNothing() throws Exception {
        final var gameId = newGameId();
        for (final var bad : new String[] {"abc", "0", "11", "", "2.5"}) {
            final var response = post("/api/games/" + gameId + "/guesses", Map.of("guess", bad));
            assertEquals(BAD_REQUEST, response.statusCode());
            assertTrue(json(response).get("error").asText().contains("whole number"));
        }
        assertEquals(BAD_REQUEST, post("/api/games/" + gameId + "/guesses", Map.of()).statusCode());
        post("/api/games/" + gameId + "/guesses", Map.of("guess", "7"));

        assertEquals(BAD_REQUEST, post("/api/games/" + gameId + "/score", Map.of("name", "   ")).statusCode());
        assertEquals(BAD_REQUEST,
                post("/api/games/" + gameId + "/score", Map.of("name", "x".repeat(GameRules.MAX_NAME_LENGTH + 1))).statusCode());
        assertEquals(0, json(get("/api/leaderboard")).get("entries").size());
        assertEquals(NOT_FOUND, post("/api/games/missing/guesses", Map.of("guess", "1")).statusCode());
    }

    @Test
    void leaderboardShowsTopTenInRankOrder() throws Exception {
        for (var index = 0; index < GameRules.LEADERBOARD_SIZE + 2; index++) {
            final var gameId = newGameId();
            post("/api/games/" + gameId + "/guesses", Map.of("guess", index % 2 == 0 ? "1" : "7"));
            if (index % 2 == 0) {
                post("/api/games/" + gameId + "/guesses", Map.of("guess", "7"));
            }
            post("/api/games/" + gameId + "/score", Map.of("name", "p" + index));
        }
        final var entries = json(get("/api/leaderboard")).get("entries");
        assertEquals(GameRules.LEADERBOARD_SIZE, entries.size());
        assertEquals("p1", entries.get(0).get("name").asText());
        assertEquals(1, entries.get(0).get("guesses").asInt());
        assertEquals(GameRules.LEADERBOARD_SIZE, entries.get(GameRules.LEADERBOARD_SIZE - 1).get("rank").asInt());
    }

    private WebServer startServer() {
        final var service = new GameService(new JdbcLeaderboardRepository(dataSource), () -> SECRET);
        return new WebServer(service).start(0);
    }

    private String newGameId() throws Exception {
        final var response = post("/api/games", null);
        assertEquals(CREATED, response.statusCode());
        return json(response).get("gameId").asText();
    }

    private HttpResponse<String> get(final String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(final String path, final Object body) throws Exception {
        final var publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body));
        return client.send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json").POST(publisher).build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(final HttpResponse<String> response) throws Exception {
        return mapper.readTree(response.body());
    }

    private URI uri(final String path) {
        return URI.create("http://localhost:" + server.port() + path);
    }
}
