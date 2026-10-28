package com.example.dukeguess.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.dukeguess.game.GameRules;
import com.example.dukeguess.game.GameStatus;
import com.example.dukeguess.game.GuessVerdict;
import com.example.dukeguess.support.DukeGuessIntegrationTest;
import com.example.dukeguess.support.FixedSecretNumberPicker;

import static com.example.dukeguess.game.GameRules.MAX_GUESSES;
import static org.assertj.core.api.Assertions.assertThat;

/// Plays rounds over HTTP against the running app, the same way the browser does.
@DukeGuessIntegrationTest
class GameApiIntegrationTest {

    private static final int SECRET = 7;
    private static final int TOO_LOW = SECRET - 1;
    private static final int TOO_HIGH = SECRET + 1;
    private static final ParameterizedTypeReference<List<StandingView>> STANDINGS =
            new ParameterizedTypeReference<>() {
            };

    @Autowired
    private FixedSecretNumberPicker secretNumberPicker;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private SimpleMeterRegistry meterRegistry;

    private RestTestClient client;

    @BeforeEach
    void startFresh(@LocalServerPort final int port) {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        jdbcClient.sql("TRUNCATE leaderboard_entry").update();
        secretNumberPicker.willPick(SECRET);
    }

    private GameView startGame() {
        return client.post().uri("/api/games")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(GameView.class)
                .returnResult().getResponseBody();
    }

    private RestTestClient.ResponseSpec guess(final UUID gameId, final Object guess) {
        return client.post().uri("/api/games/{gameId}/guesses", gameId)
                .body(Map.of("guess", guess))
                .exchange();
    }

    private GuessView acceptedGuess(final UUID gameId, final int guess) {
        return guess(gameId, guess)
                .expectStatus().isOk()
                .expectBody(GuessView.class)
                .returnResult().getResponseBody();
    }

    private RestTestClient.ResponseSpec postScore(final UUID gameId, final String name) {
        return client.post().uri("/api/games/{gameId}/score", gameId)
                .body(Map.of("name", name))
                .exchange();
    }

    private List<StandingView> standings() {
        return client.get().uri("/api/leaderboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody(STANDINGS)
                .returnResult().getResponseBody();
    }

    @Test
    void startsARoundWithEveryGuessAvailable() {
        final var game = startGame();

        assertThat(game.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(game.guessesMade()).isZero();
        assertThat(game.guessesRemaining()).isEqualTo(MAX_GUESSES);
        assertThat(game.secretNumber()).isNull();
    }

    @Test
    void tellsThePlayerWhichWayToGoAndKeepsTheTally() {
        final var gameId = startGame().gameId();

        assertThat(acceptedGuess(gameId, TOO_LOW).verdict()).isEqualTo(GuessVerdict.TOO_LOW);
        final var second = acceptedGuess(gameId, TOO_HIGH);

        assertThat(second.verdict()).isEqualTo(GuessVerdict.TOO_HIGH);
        assertThat(second.game().guessesMade()).isEqualTo(2);
        assertThat(second.game().guessesRemaining()).isEqualTo(MAX_GUESSES - 2);
        assertThat(second.game().secretNumber()).isNull();
    }

    @Test
    void endsTheRoundOnTheRightNumber() {
        final var gameId = startGame().gameId();

        final var won = acceptedGuess(gameId, SECRET);

        assertThat(won.verdict()).isEqualTo(GuessVerdict.CORRECT);
        assertThat(won.game().status()).isEqualTo(GameStatus.WON);
        assertThat(won.game().guessesMade()).isEqualTo(1);
        assertThat(won.game().secretNumber()).isEqualTo(SECRET);
    }

    @Test
    void revealsTheNumberWhenTheGuessesRunOut() {
        final var gameId = startGame().gameId();

        GuessView lastGuess = null;
        for (var attempt = 0; attempt < MAX_GUESSES; attempt++) {
            lastGuess = acceptedGuess(gameId, TOO_LOW);
        }

        assertThat(lastGuess.game().status()).isEqualTo(GameStatus.LOST);
        assertThat(lastGuess.game().guessesRemaining()).isZero();
        assertThat(lastGuess.game().secretNumber()).isEqualTo(SECRET);
        // The losing guess still says which way it missed, so the screen can tell the
        // player that before revealing the number.
        assertThat(lastGuess.verdict()).isEqualTo(GuessVerdict.TOO_LOW);
    }

    @Test
    void takesNoMoreGuessesOnceTheRoundIsOver() {
        final var gameId = startGame().gameId();
        acceptedGuess(gameId, SECRET);

        guess(gameId, TOO_LOW)
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("game-over");
    }

    @Test
    void turnsAwayAGuessOutsideTheRangeWithoutSpendingOne() {
        final var gameId = startGame().gameId();

        guess(gameId, GameRules.HIGHEST_NUMBER + 1)
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("invalid-guess");

        assertThat(acceptedGuess(gameId, TOO_LOW).game().guessesMade()).isEqualTo(1);
    }

    @Test
    void turnsAwayAGuessThatIsNotAWholeNumber() {
        final var gameId = startGame().gameId();

        guess(gameId, "banana").expectStatus().isBadRequest();
        guess(gameId, 3.5).expectStatus().isBadRequest();

        assertThat(acceptedGuess(gameId, TOO_LOW).game().guessesMade()).isEqualTo(1);
    }

    @Test
    void answersWithNotFoundForARoundTheServerDoesNotHold() {
        guess(UUID.randomUUID(), SECRET)
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.code").isEqualTo("game-not-found");
    }

    @Test
    void putsAWinOnTheLeaderboardUnderTheGuessCountTheServerScored() {
        final var gameId = startGame().gameId();
        acceptedGuess(gameId, TOO_LOW);
        acceptedGuess(gameId, SECRET);

        postScore(gameId, "  Duke  ")
                .expectStatus().isCreated()
                .expectBody(PostedScoreView.class)
                .value(posted -> {
                    assertThat(posted.name()).isEqualTo("Duke");
                    assertThat(posted.guesses()).isEqualTo(2);
                });

        assertThat(standings()).containsExactly(new StandingView(1, "Duke", 2));
    }

    @Test
    void refusesASecondPostOfTheSameWin() {
        final var gameId = startGame().gameId();
        acceptedGuess(gameId, SECRET);
        postScore(gameId, "Duke").expectStatus().isCreated();

        postScore(gameId, "Duke again")
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("score-already-posted");

        assertThat(standings()).hasSize(1);
    }

    @Test
    void refusesANameWithoutAVisibleCharacter() {
        final var gameId = startGame().gameId();
        acceptedGuess(gameId, SECRET);

        postScore(gameId, "   ")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("invalid-name");

        assertThat(standings()).isEmpty();
    }

    @Test
    void refusesANameLongerThanTheLimit() {
        final var gameId = startGame().gameId();
        acceptedGuess(gameId, SECRET);

        postScore(gameId, "x".repeat(31))
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.code").isEqualTo("invalid-name");

        assertThat(standings()).isEmpty();
    }

    @Test
    void recordsNothingForALostRound() {
        final var gameId = startGame().gameId();
        for (var attempt = 0; attempt < MAX_GUESSES; attempt++) {
            acceptedGuess(gameId, TOO_LOW);
        }

        postScore(gameId, "Duke")
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.code").isEqualTo("game-not-won");

        assertThat(standings()).isEmpty();
    }

    @Test
    void ranksByFewestGuessesAndBreaksTiesByWhoPostedFirst() {
        postWinIn(3, "Three");
        postWinIn(1, "First in one");
        postWinIn(1, "Second in one");

        assertThat(standings()).containsExactly(
                new StandingView(1, "First in one", 1),
                new StandingView(2, "Second in one", 1),
                new StandingView(3, "Three", 3));
    }

    @Test
    void showsNothingOnTheLeaderboardUntilSomeoneWins() {
        assertThat(standings()).isEmpty();
    }

    @Test
    void countsTheRoundsItPlayedWithoutRecordingWhoPlayedThem() {
        final var startedBefore = counterValue("duke.games.started");

        final var gameId = startGame().gameId();
        acceptedGuess(gameId, SECRET);
        postScore(gameId, "Telemetry Tess").expectStatus().isCreated();

        assertThat(counterValue("duke.games.started")).isEqualTo(startedBefore + 1);
        assertThat(meterRegistry.find("duke.games.finished").tag("outcome", "won").counter()).isNotNull();
        assertThat(meterRegistry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .extracting(Tag::getValue)
                .doesNotContain("Telemetry Tess");
    }

    @Test
    void servesAnOpenApiDefinitionCoveringItsEndpoints() {
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['/api/games']").exists()
                .jsonPath("$.paths['/api/games/{gameId}/guesses']").exists()
                .jsonPath("$.paths['/api/games/{gameId}/score']").exists()
                .jsonPath("$.paths['/api/leaderboard']").exists()
                .jsonPath("$.paths['/api/games/{gameId}/guesses'].post.responses.404").exists();
    }

    @Test
    void servesBrowsableApiDocumentation() {
        client.get().uri("/swagger-ui/index.html").exchange().expectStatus().isOk();
    }

    private void postWinIn(final int guesses, final String name) {
        final var gameId = startGame().gameId();
        for (var attempt = 1; attempt < guesses; attempt++) {
            acceptedGuess(gameId, TOO_LOW);
        }
        acceptedGuess(gameId, SECRET);
        postScore(gameId, name).expectStatus().isCreated();
    }

    private double counterValue(final String name) {
        final var counter = meterRegistry.find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }
}
