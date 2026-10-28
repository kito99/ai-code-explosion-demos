package com.example.game;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the game and leaderboard REST endpoints.
 * Uses the built-in Java HTTP client — no extra test dependencies needed.
 * Run via `mvn verify`; liberty-maven-plugin starts/stops the server.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class GameRestIT {

    private static final int PORT = Integer.getInteger("liberty.http.port", 9080);
    private static final String BASE = "http://localhost:" + PORT;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    // ---- Game lifecycle ----

    @Test
    void t01_newGame_returnsActiveState() throws Exception {
        var resp = post("/api/game", "");
        assertEquals(200, resp.statusCode(), "Expected 200 from POST /api/game");
        var body = resp.body();
        assertTrue(body.contains("\"status\":\"ACTIVE\""), "Expected ACTIVE status in: " + body);
        assertTrue(body.contains("\"guessCount\":0"), "Expected guessCount=0 in: " + body);
        assertTrue(body.contains("\"guessesRemaining\":5"), "Expected guessesRemaining=5 in: " + body);
        assertTrue(body.contains("\"sessionId\""), "Expected sessionId in: " + body);
    }

    @Test
    void t02_guess_outOfRange_returns400() throws Exception {
        var sessionId = startGame();
        var resp = post("/api/game/" + sessionId + "/guess", "{\"number\":99}");
        assertEquals(400, resp.statusCode(), "Expected 400 for out-of-range guess");
    }

    @Test
    void t03_guess_unknownSession_returns404() throws Exception {
        var resp = post("/api/game/no-such-session/guess", "{\"number\":5}");
        assertEquals(404, resp.statusCode(), "Expected 404 for unknown session");
    }

    @Test
    void t04_leaderboard_initiallyReturns200() throws Exception {
        var resp = get("/api/leaderboard");
        assertEquals(200, resp.statusCode(), "Expected 200 from GET /api/leaderboard");
        var body = resp.body();
        assertTrue(body.startsWith("["), "Expected JSON array from leaderboard: " + body);
    }

    @Test
    void t05_postScore_emptyName_returns400() throws Exception {
        var resp = post("/api/leaderboard", "{\"playerName\":\"\",\"guessCount\":3}");
        assertEquals(400, resp.statusCode(), "Expected 400 for empty name");
    }

    @Test
    void t06_postScore_validEntry_returns201() throws Exception {
        var resp = post("/api/leaderboard", "{\"playerName\":\"Duke Fan\",\"guessCount\":2}");
        assertEquals(201, resp.statusCode(), "Expected 201 for valid score post");
    }

    @Test
    void t07_leaderboard_afterPost_containsEntry() throws Exception {
        post("/api/leaderboard", "{\"playerName\":\"TestPlayer\",\"guessCount\":1}");

        var resp = get("/api/leaderboard");
        assertEquals(200, resp.statusCode());
        var body = resp.body();
        assertTrue(body.contains("\"playerName\""), "Expected playerName field in: " + body);
        assertTrue(body.contains("\"rank\""), "Expected rank field in: " + body);
        assertTrue(body.contains("\"guessCount\""), "Expected guessCount field in: " + body);
    }

    @Test
    void t08_leaderboard_orderedByFewestGuesses() throws Exception {
        // Post multiple scores with different guess counts
        post("/api/leaderboard", "{\"playerName\":\"Alice\",\"guessCount\":3}");
        post("/api/leaderboard", "{\"playerName\":\"Bob\",\"guessCount\":1}");
        post("/api/leaderboard", "{\"playerName\":\"Carol\",\"guessCount\":2}");

        var resp = get("/api/leaderboard");
        var body = resp.body();
        // Bob (1 guess) should rank above Carol (2) and Alice (3)
        var bobIdx = body.indexOf("Bob");
        var carolIdx = body.indexOf("Carol");
        var aliceIdx = body.indexOf("Alice");
        assertTrue(bobIdx < carolIdx, "Bob (1 guess) should appear before Carol (2 guesses)");
        assertTrue(carolIdx < aliceIdx, "Carol (2 guesses) should appear before Alice (3 guesses)");
    }

    // --- Helpers ---

    private String startGame() throws Exception {
        var resp = post("/api/game", "");
        var body = resp.body();
        // Extract sessionId from JSON: "sessionId":"<uuid>"
        var idx = body.indexOf("\"sessionId\":\"") + "\"sessionId\":\"".length();
        var end = body.indexOf("\"", idx);
        return body.substring(idx, end);
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .header("Accept", "application/json")
                .GET()
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String jsonBody) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
