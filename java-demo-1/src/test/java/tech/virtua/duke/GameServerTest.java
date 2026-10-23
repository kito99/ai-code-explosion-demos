package tech.virtua.duke;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end checks against a real server, cookies and all. */
class GameServerTest {

    /** Duke always thinks of 7 in these tests. */
    private static final RandomGenerator ALWAYS_SEVEN = new RandomGenerator() {
        @Override
        public long nextLong() {
            return 7L;
        }

        @Override
        public int nextInt(int origin, int bound) {
            return 7;
        }
    };

    private static App.GameServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = App.start(0, ALWAYS_SEVEN);
        baseUrl = "http://localhost:" + server.port();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    private static HttpClient player() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    private static HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(HttpClient client, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> guess(HttpClient client, String value) throws Exception {
        return post(client, "/api/game/guess", "guess=" + value);
    }

    /** Pulls one member out of a JSON object without dragging in a parser. */
    private static String field(String json, String name) {
        Matcher matcher = Pattern.compile("\"" + name + "\":(\"(?:[^\"\\\\]|\\\\.)*\"|null|true|false|-?\\d+)")
                .matcher(json);
        assertTrue(matcher.find(), "no member named " + name + " in " + json);
        String raw = matcher.group(1);
        return raw.startsWith("\"") ? raw.substring(1, raw.length() - 1) : raw;
    }

    @Test
    @DisplayName("The page, its stylesheet and its script are all served")
    void frontEndIsServed() throws Exception {
        HttpClient client = player();

        HttpResponse<String> page = get(client, "/");
        HttpResponse<String> css = get(client, "/styles.css");
        HttpResponse<String> js = get(client, "/app.js");

        assertAll(
                () -> assertEquals(200, page.statusCode()),
                () -> assertTrue(page.headers().firstValue("Content-Type").orElse("").startsWith("text/html")),
                () -> assertTrue(page.body().contains("Guess the Number")),
                () -> assertTrue(page.body().contains("duke"), "Duke should be on the page"),
                () -> assertEquals(200, css.statusCode()),
                () -> assertEquals(200, js.statusCode()));
    }

    @Test
    @DisplayName("FR-1: loading the game hands back a fresh round")
    void loadingTheGameStartsARound() throws Exception {
        String body = get(player(), "/api/game").body();

        assertAll(
                () -> assertEquals("PLAYING", field(body, "status")),
                () -> assertEquals("0", field(body, "guessesMade")),
                () -> assertEquals("5", field(body, "guessesRemaining")),
                () -> assertEquals("null", field(body, "secret")));
    }

    @Test
    @DisplayName("FR-7 / FR-9: guesses get high-low feedback and a win is recognized")
    void playingThroughToAWin() throws Exception {
        HttpClient client = player();
        get(client, "/api/game");

        String high = guess(client, "9").body();
        String low = guess(client, "2").body();
        String won = guess(client, "7").body();

        assertAll(
                () -> assertEquals("TOO_HIGH", field(high, "hint")),
                () -> assertEquals("TOO_LOW", field(low, "hint")),
                () -> assertEquals("WON", field(won, "status")),
                () -> assertEquals("3", field(won, "guessesMade")),
                () -> assertTrue(field(won, "message").contains("You got it")));
    }

    @Test
    @DisplayName("FR-11: five misses end the game and reveal the secret")
    void playingThroughToALoss() throws Exception {
        HttpClient client = player();
        get(client, "/api/game");

        String last = null;
        for (String value : new String[] {"1", "2", "3", "4", "5"}) {
            last = guess(client, value).body();
        }

        String body = last;
        assertAll(
                () -> assertEquals("LOST", field(body, "status")),
                () -> assertEquals("0", field(body, "guessesRemaining")),
                () -> assertEquals("7", field(body, "secret")),
                () -> assertTrue(field(body, "message").contains("too low"),
                        "the losing guess still needs its high-low feedback"));
    }

    @Test
    @DisplayName("FR-6: bad input comes back with a friendly message and no guess spent")
    void badInputIsRejected() throws Exception {
        HttpClient client = player();
        get(client, "/api/game");
        guess(client, "1");

        String body = guess(client, "banana").body();

        assertAll(
                () -> assertEquals("PLAYING", field(body, "status")),
                () -> assertEquals("1", field(body, "guessesMade")),
                () -> assertEquals("4", field(body, "guessesRemaining")),
                () -> assertEquals("false", field(body, "guessAccepted")));
    }

    @Test
    @DisplayName("FR-6: an empty submission is rejected too")
    void emptySubmissionIsRejected() throws Exception {
        HttpClient client = player();
        get(client, "/api/game");

        String body = post(client, "/api/game/guess", "").body();

        assertAll(
                () -> assertEquals("PLAYING", field(body, "status")),
                () -> assertEquals("0", field(body, "guessesMade")));
    }

    @Test
    @DisplayName("FR-12: play again clears the round and restores all five guesses")
    void playAgainResetsTheRound() throws Exception {
        HttpClient client = player();
        get(client, "/api/game");
        guess(client, "3");
        guess(client, "7");

        String body = post(client, "/api/game/new", "").body();

        assertAll(
                () -> assertEquals("PLAYING", field(body, "status")),
                () -> assertEquals("0", field(body, "guessesMade")),
                () -> assertEquals("5", field(body, "guessesRemaining")),
                () -> assertEquals("null", field(body, "lastGuess")),
                () -> assertEquals("NONE", field(body, "hint")),
                () -> assertTrue(field(body, "message").contains("thinking of a number")));
    }

    @Test
    @DisplayName("FR-12: a game can be restarted mid-round")
    void playAgainWorksMidRound() throws Exception {
        HttpClient client = player();
        get(client, "/api/game");
        guess(client, "3");

        String body = post(client, "/api/game/new", "").body();

        assertEquals("0", field(body, "guessesMade"));
    }

    @Test
    @DisplayName("FR-1: reloading the page deals a brand new game")
    void reloadingThePageStartsOver() throws Exception {
        HttpClient client = player();
        get(client, "/");
        post(client, "/api/game/new", "");
        guess(client, "3");
        assertEquals("1", field(get(client, "/api/game").body(), "guessesMade"));

        // what the page itself does on load
        String reloaded = post(client, "/api/game/new", "").body();

        assertEquals("0", field(reloaded, "guessesMade"));
    }

    @Test
    @DisplayName("Each browser gets its own game")
    void sessionsAreIndependent() throws Exception {
        HttpClient first = player();
        HttpClient second = player();

        get(first, "/api/game");
        guess(first, "1");
        guess(first, "2");

        String firstState = get(first, "/api/game").body();
        String secondState = get(second, "/api/game").body();

        assertAll(
                () -> assertEquals("2", field(firstState, "guessesMade")),
                () -> assertEquals("0", field(secondState, "guessesMade")));
    }

    @Test
    @DisplayName("The session survives across requests")
    void sessionCookieIsIssuedAndReused() throws Exception {
        HttpClient client = player();

        HttpResponse<String> first = get(client, "/api/game");
        assertTrue(first.headers().firstValue("Set-Cookie").orElse("").contains(ApiHandler.COOKIE_NAME));

        guess(client, "1");
        assertEquals("1", field(get(client, "/api/game").body(), "guessesMade"));
    }

    @Test
    @DisplayName("Unknown paths and wrong methods fail politely")
    void unknownPathsAndMethodsAreHandled() throws Exception {
        HttpClient client = player();

        HttpResponse<String> missingApi = get(client, "/api/nope");
        HttpResponse<String> missingPage = get(client, "/nope.html");
        HttpResponse<String> wrongMethod = get(client, "/api/game/guess");

        assertAll(
                () -> assertEquals(404, missingApi.statusCode()),
                () -> assertEquals(404, missingPage.statusCode()),
                () -> assertEquals(405, wrongMethod.statusCode()),
                () -> assertEquals("POST", wrongMethod.headers().firstValue("Allow").orElse("")));
    }

    @Test
    @DisplayName("An oversized body is refused outright rather than truncated into a guess")
    void oversizedBodyCannotBecomeAGuess() throws Exception {
        HttpClient client = player();
        get(client, "/api/game");

        // Truncating this at 1024 bytes would leave a trailing "guess=1".
        String padded = "x=" + "a".repeat(1014) + "&guess=11";
        String body = post(client, "/api/game/guess", padded).body();

        assertAll(
                () -> assertEquals("PLAYING", field(body, "status")),
                () -> assertEquals("0", field(body, "guessesMade")),
                () -> assertEquals("false", field(body, "guessAccepted")));
    }

    @Test
    @DisplayName("Overlapping first requests on one session cannot lose an accepted guess")
    void concurrentStartsShareOneGame() throws Exception {
        String id = UUID.randomUUID().toString();
        int guessers = 4;
        ExecutorService pool = Executors.newFixedThreadPool(guessers);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        // Every one of these is the session's first request, so each could try to create it.
        for (int i = 0; i < guessers; i++) {
            futures.add(pool.submit(() -> {
                go.await();
                return sendWithSession(id, "/api/game/guess", "guess=1");
            }));
        }
        go.countDown();
        for (Future<String> future : futures) {
            future.get();
        }
        pool.shutdown();

        String finalState = sendWithSession(id, "/api/game", null);

        assertEquals(String.valueOf(guessers), field(finalState, "guessesMade"),
                "every accepted guess should have landed on the same game");
    }

    private static String sendWithSession(String sessionId, String path, String formBody) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Cookie", ApiHandler.COOKIE_NAME + "=" + sessionId);
        if (formBody == null) {
            request.GET();
        } else {
            request.header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody));
        }
        return HttpClient.newHttpClient()
                .send(request.build(), HttpResponse.BodyHandlers.ofString())
                .body();
    }

    @Test
    @DisplayName("A forged session cookie is ignored rather than trusted")
    void forgedSessionCookieIsIgnored() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/game"))
                        .header("Cookie", ApiHandler.COOKIE_NAME + "=not-a-real-session")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertEquals("PLAYING", field(response.body(), "status")),
                () -> assertTrue(response.headers().firstValue("Set-Cookie").orElse("")
                        .contains(ApiHandler.COOKIE_NAME + "="), "a clean session id should be issued"),
                () -> assertNotEquals(true, response.headers().firstValue("Set-Cookie").orElse("")
                        .contains("not-a-real-session")));
    }
}
