package com.example.duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/**
 * Runs the packaged app on a real Open Liberty server against a throwaway PostgreSQL container.
 * The server and database are provisioned here, so no setup is needed on the machine.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GameApiIT {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6");
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static int port;
    static Process server;

    @BeforeAll
    static void startAll() throws Exception {
        POSTGRES.start();
        port = freePort();
        startServer();
    }

    @AfterAll
    static void stopAll() throws Exception {
        stopServer();
        POSTGRES.stop();
    }

    static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    static void startServer() throws Exception {
        Path home = Path.of(System.getProperty("liberty.home"));
        ProcessBuilder pb = new ProcessBuilder(home.resolve("bin/server").toString(), "run", "defaultServer");
        pb.environment().put("HTTP_PORT", String.valueOf(port));
        pb.environment().put("DB_URL", POSTGRES.getJdbcUrl());
        pb.environment().put("DB_USER", POSTGRES.getUsername());
        pb.environment().put("DB_PASSWORD", POSTGRES.getPassword());
        File log = home.getParent().resolve("liberty-it.log").toFile();
        pb.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        server = pb.start();

        long deadline = System.currentTimeMillis() + Duration.ofMinutes(3).toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!server.isAlive()) {
                throw new IllegalStateException("Liberty exited early, see " + log);
            }
            try {
                if (send("GET", "/health/ready", null).statusCode() == 200) {
                    return;
                }
            } catch (IOException notUpYet) {
                // keep polling
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("Liberty did not become ready, see " + log);
    }

    static void stopServer() throws Exception {
        if (server != null && server.isAlive()) {
            server.destroy();
            server.waitFor();
        }
    }

    static HttpResponse<String> send(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    static JsonObject json(HttpResponse<String> response) {
        return Json.createReader(new StringReader(response.body())).readObject();
    }

    static JsonObject startGame() throws Exception {
        HttpResponse<String> r = send("POST", "/api/games", null);
        assertEquals(201, r.statusCode());
        return json(r);
    }

    static JsonObject guess(String gameId, String text) throws Exception {
        return json(send("POST", "/api/games/" + gameId + "/guesses", "{\"guess\":\"" + text + "\"}"));
    }

    /** Binary search always wins in at most four guesses; returns the final guess response. */
    static JsonObject playToWin(String gameId) throws Exception {
        int low = 1;
        int high = 10;
        while (true) {
            int mid = (low + high) / 2;
            JsonObject r = guess(gameId, String.valueOf(mid));
            switch (r.getString("hint")) {
                case "CORRECT" -> {
                    return r;
                }
                case "TOO_LOW" -> low = mid + 1;
                default -> high = mid - 1;
            }
        }
    }

    static List<String> leaderboardRows() throws Exception {
        JsonArray rows = Json.createReader(new StringReader(send("GET", "/api/leaderboard", null).body())).readArray();
        List<String> out = new ArrayList<>();
        rows.forEach(v -> {
            JsonObject o = v.asJsonObject();
            out.add(o.getInt("rank") + ":" + o.getString("name") + ":" + o.getInt("guesses"));
        });
        return out;
    }

    @Test
    @Order(1)
    void emptyLeaderboardHasNoRows() throws Exception {
        assertTrue(leaderboardRows().isEmpty());
    }

    @Test
    @Order(2)
    void newGameStartsWithFiveGuessesAndNoSecret() throws Exception {
        JsonObject game = startGame();
        assertEquals("ACTIVE", game.getString("status"));
        assertEquals(0, game.getInt("guessesMade"));
        assertEquals(5, game.getInt("guessesRemaining"));
        assertFalse(game.containsKey("secretNumber") && !game.isNull("secretNumber"));
    }

    @Test
    @Order(3)
    void invalidGuessesAreRejectedWithoutCounting() throws Exception {
        String id = startGame().getString("id");
        for (String bad : new String[] {"0", "11", "abc", "3.5", ""}) {
            var r = send("POST", "/api/games/" + id + "/guesses", "{\"guess\":\"" + bad + "\"}");
            assertEquals(400, r.statusCode(), bad);
            assertEquals("Please enter a whole number from 1 to 10.", json(r).getString("message"));
        }
        assertEquals(400, send("POST", "/api/games/" + id + "/guesses", "{}").statusCode());
        assertEquals(400, send("POST", "/api/games/" + id + "/guesses", "not json").statusCode());
        JsonObject ok = guess(id, "5");
        assertEquals(1, ok.getJsonObject("game").getInt("guessesMade"));
    }

    @Test
    @Order(4)
    void losingRevealsSecretAndCannotBePosted() throws Exception {
        JsonObject last = null;
        String id = null;
        for (int attempt = 0; attempt < 50 && (last == null || !"LOST".equals(last.getJsonObject("game").getString("status"))); attempt++) {
            id = startGame().getString("id");
            for (int i = 0; i < 5; i++) {
                last = guess(id, "1");
                if ("CORRECT".equals(last.getString("hint"))) {
                    break;
                }
            }
        }
        JsonObject game = last.getJsonObject("game");
        assertEquals("LOST", game.getString("status"));
        assertEquals(0, game.getInt("guessesRemaining"));
        assertTrue(game.getInt("secretNumber") >= 2);
        assertEquals(409, send("POST", "/api/games/" + id + "/guesses", "{\"guess\":\"2\"}").statusCode());
        assertEquals(409, send("POST", "/api/games/" + id + "/score", "{\"name\":\"Loser\"}").statusCode());
        assertEquals(List.of(), leaderboardRows());
    }

    @Test
    @Order(5)
    void winningAndPostingValidatesTheNameAndRecordsOnce() throws Exception {
        String id = startGame().getString("id");
        JsonObject win = playToWin(id);
        int guesses = win.getJsonObject("game").getInt("guessesMade");
        assertEquals("WON", win.getJsonObject("game").getString("status"));
        assertEquals(409, send("POST", "/api/games/" + id + "/guesses", "{\"guess\":\"1\"}").statusCode());

        for (String bad : new String[] {"   ", "", "x".repeat(31)}) {
            assertEquals(400, send("POST", "/api/games/" + id + "/score", "{\"name\":\"" + bad + "\"}").statusCode(), bad);
        }
        assertEquals(List.of(), leaderboardRows());

        assertEquals(201, send("POST", "/api/games/" + id + "/score", "{\"name\":\"  Duke  \"}").statusCode());
        assertEquals(List.of("1:Duke:" + guesses), leaderboardRows());
        assertEquals(409, send("POST", "/api/games/" + id + "/score", "{\"name\":\"Duke\"}").statusCode());
        assertEquals(404, send("POST", "/api/games/unknown/score", "{\"name\":\"Duke\"}").statusCode());
    }

    @Test
    @Order(6)
    void leaderboardShowsTopTenFewestGuessesFirstWithTiesByRecordedOrder() throws Exception {
        for (int i = 0; i < 12; i++) {
            String id = startGame().getString("id");
            playToWin(id);
            assertEquals(201, send("POST", "/api/games/" + id + "/score", "{\"name\":\"p" + i + "\"}").statusCode());
        }
        List<String> rows = leaderboardRows();
        assertEquals(10, rows.size());
        int previousGuesses = 0;
        int previousIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            String[] parts = rows.get(i).split(":");
            assertEquals(String.valueOf(i + 1), parts[0]);
            int guesses = Integer.parseInt(parts[2]);
            assertTrue(guesses >= previousGuesses, rows.toString());
            int index = parts[1].equals("Duke") ? -1 : Integer.parseInt(parts[1].substring(1));
            if (guesses == previousGuesses && !parts[1].equals("Duke")) {
                assertTrue(index > previousIndex, "ties must stay in recorded order: " + rows);
            }
            previousGuesses = guesses;
            previousIndex = index;
        }
    }

    @Test
    @Order(7)
    void leaderboardSurvivesARestart() throws Exception {
        List<String> before = leaderboardRows();
        assertFalse(before.isEmpty());
        stopServer();
        startServer();
        assertEquals(before, leaderboardRows());
        assertNotNull(startGame().getString("id"));
    }
}
