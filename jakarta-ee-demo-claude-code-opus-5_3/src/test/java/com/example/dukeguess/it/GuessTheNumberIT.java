package com.example.dukeguess.it;

import com.example.dukeguess.game.GameStatus;
import com.example.dukeguess.game.GuessOutcome;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.dukeguess.game.GameStatus.ACTIVE;
import static com.example.dukeguess.game.GameStatus.LOST;
import static com.example.dukeguess.game.GameStatus.WON;
import static com.example.dukeguess.game.GuessOutcome.CORRECT;
import static com.example.dukeguess.game.GuessOutcome.TOO_HIGH;
import static com.example.dukeguess.game.GuessOutcome.TOO_LOW;
import static com.example.dukeguess.it.GameClient.bodyOf;
import static com.example.dukeguess.leaderboard.LeaderboardService.LEADERBOARD_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static java.util.stream.IntStream.rangeClosed;

/// End-to-end coverage of the game and leaderboard flows against a running server.
/// The leaderboard starts empty on every server start, so the empty-board check runs first.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GuessTheNumberIT {

    private static final int MAXIMUM_GUESSES = 5;
    /// Each game draws a fresh secret, so a helper that needs a particular result retries.
    private static final int MAXIMUM_ATTEMPTS = 100;
    private static final int LOWEST_NUMBER = 1;
    private static final int HIGHEST_NUMBER = 10;

    @Test
    @Order(1)
    void leaderboardExplainsItselfWhenNothingIsPosted() {
        try (final var player = new GameClient()) {
            final var body = bodyOf(player.get("api/leaderboard"));

            assertThat(results(body)).isEmpty();
            assertThat(body.get("message").toString()).contains("No scores have been posted");
        }
    }

    @Test
    @Order(2)
    void winningAndNamingYourselfPutsYouOnTheLeaderboard() {
        final var playerName = uniqueName();
        try (final var player = new GameClient()) {
            final var win = playUntilWon(player);
            final var guessCount = intValue(win, "guessesMade");

            final var posted = player.post("api/leaderboard", Map.of("playerName", " %s ".formatted(playerName)));
            assertThat(posted.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
            assertThat(bodyOf(posted).get("message").toString()).contains(playerName);

            final var leaderboard = results(bodyOf(player.get("api/leaderboard")));
            assertThat(leaderboard)
                    .anySatisfy(entry -> {
                        assertThat(entry.get("playerName")).isEqualTo(playerName);
                        assertThat(intValue(entry, "guessCount")).isEqualTo(guessCount);
                    });
            assertThat(leaderboard).extracting(entry -> intValue(entry, "guessCount")).isSorted();
            assertThat(leaderboard).hasSizeLessThanOrEqualTo(LEADERBOARD_SIZE);
            assertThat(leaderboard).extracting(entry -> intValue(entry, "rank"))
                    .containsExactlyElementsOf(ranksUpTo(leaderboard.size()));
        }
    }

    @Test
    @Order(3)
    void aWinIsPostedOnlyOnce() {
        try (final var player = new GameClient()) {
            playUntilWon(player);
            player.post("api/leaderboard", Map.of("playerName", uniqueName()));

            final var repeated = player.post("api/leaderboard", Map.of("playerName", uniqueName()));

            assertThat(repeated.getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
        }
    }

    @Test
    @Order(4)
    void aBlankNameRecordsNothingAndLeavesTheWinPostable() {
        final var playerName = uniqueName();
        try (final var player = new GameClient()) {
            playUntilWon(player);

            final var rejected = player.post("api/leaderboard", Map.of("playerName", "   "));
            assertThat(rejected.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(bodyOf(rejected).get("message").toString()).contains("name");
            assertThat(results(bodyOf(player.get("api/leaderboard"))))
                    .extracting(entry -> entry.get("playerName"))
                    .doesNotContain("", "   ");

            final var accepted = player.post("api/leaderboard", Map.of("playerName", playerName));
            assertThat(accepted.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        }
    }

    @Test
    @Order(5)
    void guessesThatAreNotWholeNumbersInRangeDoNotCount() {
        try (final var player = new GameClient()) {
            player.post("api/game/new", null);

            for (final var typedGuess : List.of("duke", "", "0", "11", "2.5")) {
                final var rejected = player.post("api/game/guesses", Map.of("guess", typedGuess));

                assertThat(rejected.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
                assertThat(bodyOf(rejected).get("message").toString()).contains("whole number");
            }

            final var state = bodyOf(player.get("api/game"));
            assertThat(intValue(state, "guessesMade")).isZero();
            assertThat(intValue(state, "guessesRemaining")).isEqualTo(MAXIMUM_GUESSES);
            assertThat(statusOf(state)).isEqualTo(ACTIVE);
        }
    }

    @Test
    @Order(6)
    void runningOutOfGuessesRevealsTheSecretNumberAndRecordsNothing() {
        try (final var player = new GameClient()) {
            final var loss = playUntilLost(player);

            assertThat(statusOf(loss)).isEqualTo(LOST);
            assertThat(intValue(loss, "guessesMade")).isEqualTo(MAXIMUM_GUESSES);
            assertThat(intValue(loss, "guessesRemaining")).isZero();
            assertThat(intValue(loss, "secretNumber")).isBetween(LOWEST_NUMBER, HIGHEST_NUMBER);
            assertThat(loss.get("message").toString()).contains("Out of guesses");

            final var lateGuess = player.post("api/game/guesses", Map.of("guess", "1"));
            assertThat(lateGuess.getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());

            final var refused = player.post("api/leaderboard", Map.of("playerName", uniqueName()));
            assertThat(refused.getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
        }
    }

    @Test
    @Order(7)
    void startingANewGameClearsTheCountsAndTheResult() {
        try (final var player = new GameClient()) {
            playUntilLost(player);

            final var fresh = bodyOf(player.post("api/game/new", null));

            assertThat(statusOf(fresh)).isEqualTo(ACTIVE);
            assertThat(intValue(fresh, "guessesMade")).isZero();
            assertThat(intValue(fresh, "guessesRemaining")).isEqualTo(MAXIMUM_GUESSES);
            assertThat(fresh.get("secretNumber")).isNull();
            assertThat(fresh.get("message").toString()).contains("thinking of a number");
        }
    }

    @Test
    @Order(8)
    void playersDoNotShareAGameButDoShareTheLeaderboard() {
        final var firstPlayerName = uniqueName();
        try (final var firstPlayer = new GameClient(); final var secondPlayer = new GameClient()) {
            playUntilWon(firstPlayer);
            firstPlayer.post("api/leaderboard", Map.of("playerName", firstPlayerName));

            final var secondPlayerGame = bodyOf(secondPlayer.get("api/game"));
            assertThat(statusOf(secondPlayerGame)).isEqualTo(ACTIVE);
            assertThat(intValue(secondPlayerGame, "guessesMade")).isZero();

            assertThat(results(bodyOf(secondPlayer.get("api/leaderboard"))))
                    .extracting(entry -> entry.get("playerName"))
                    .contains(firstPlayerName);
        }
    }

    @Test
    @Order(9)
    void theGameAndLeaderboardScreensAndDukeAreServed() {
        try (final var player = new GameClient()) {
            assertThat(player.get("").readEntity(String.class)).contains("Guess the Number with Duke");
            assertThat(player.get("leaderboard.html").readEntity(String.class)).contains("Leaderboard");
            assertThat(player.get("images/duke-thinking.svg").getStatus()).isEqualTo(200);
            assertThat(player.get("images/duke-wave.svg").getStatus()).isEqualTo(200);
        }
    }

    @Test
    @Order(10)
    void theAppReportsItselfReady() {
        try (final var player = new GameClient()) {
            final var health = player.get("health/ready");

            assertThat(health.getStatus()).isEqualTo(200);
            assertThat(health.readEntity(String.class)).contains("\"status\":\"UP\"");
        }
    }

    @Test
    @Order(11)
    void theLeaderboardShowsTenResultsRankedByFewestGuessesThenByWhoPostedFirst() {
        final var postedInOrder = new ArrayList<Map<String, Object>>();
        rangeClosed(1, LEADERBOARD_SIZE + 2).forEach(_ -> {
            try (final var player = new GameClient()) {
                final var win = playUntilWon(player);
                final var playerName = uniqueName();
                player.post("api/leaderboard", Map.of("playerName", playerName));
                postedInOrder.add(Map.of("playerName", playerName, "guessCount", intValue(win, "guessesMade")));
            }
        });

        try (final var reader = new GameClient()) {
            final var leaderboard = results(bodyOf(reader.get("api/leaderboard")));

            assertThat(leaderboard).hasSize(LEADERBOARD_SIZE);
            assertThat(leaderboard).extracting(entry -> intValue(entry, "guessCount")).isSorted();
            assertThat(leaderboard).extracting(entry -> intValue(entry, "rank"))
                    .containsExactlyElementsOf(ranksUpTo(LEADERBOARD_SIZE));

            final var postedNames = postedInOrder.stream().map(entry -> entry.get("playerName")).toList();
            final var shownNames = leaderboard.stream()
                    .map(entry -> entry.get("playerName"))
                    .filter(postedNames::contains)
                    .toList();
            assertThat(shownNames).isSortedAccordingTo((left, right) -> {
                final var leftEntry = postedInOrder.get(postedNames.indexOf(left));
                final var rightEntry = postedInOrder.get(postedNames.indexOf(right));
                final var byGuessCount = Integer.compare(
                        (int) leftEntry.get("guessCount"), (int) rightEntry.get("guessCount"));
                return byGuessCount != 0
                        ? byGuessCount
                        : Integer.compare(postedNames.indexOf(left), postedNames.indexOf(right));
            });
        }
    }

    /// Plays a binary search, which finds any number from 1 to 10 within the guess limit.
    private static Map<String, Object> playUntilWon(final GameClient player) {
        var state = bodyOf(player.post("api/game/new", null));
        var lowestPossible = LOWEST_NUMBER;
        var highestPossible = HIGHEST_NUMBER;

        for (var attempt = 0; attempt < MAXIMUM_GUESSES && statusOf(state) != WON; attempt++) {
            final var guess = (lowestPossible + highestPossible) / 2;
            state = bodyOf(player.post("api/game/guesses", Map.of("guess", String.valueOf(guess))));
            final var outcome = outcomeOf(state);
            if (outcome == TOO_LOW) {
                lowestPossible = guess + 1;
            } else if (outcome == TOO_HIGH) {
                highestPossible = guess - 1;
            }
        }
        assertThat(statusOf(state)).isEqualTo(WON);
        return state;
    }

    /// Finds a number the secret is not, then spends every guess on it.
    private static Map<String, Object> playUntilLost(final GameClient player) {
        Map<String, Object> state = null;
        for (var attempt = 0; attempt < MAXIMUM_ATTEMPTS; attempt++) {
            player.post("api/game/new", null);
            state = bodyOf(player.post("api/game/guesses", Map.of("guess", "1")));
            if (outcomeOf(state) != CORRECT) {
                break;
            }
        }

        for (var guess = 1; guess < MAXIMUM_GUESSES && statusOf(state) == ACTIVE; guess++) {
            state = bodyOf(player.post("api/game/guesses", Map.of("guess", "1")));
        }
        assertThat(statusOf(state)).isEqualTo(LOST);
        return state;
    }

    private static GameStatus statusOf(final Map<String, Object> state) {
        return GameStatus.valueOf(String.valueOf(state.get("status")));
    }

    private static GuessOutcome outcomeOf(final Map<String, Object> state) {
        final var outcome = state.get("outcome");
        return outcome == null ? null : GuessOutcome.valueOf(String.valueOf(outcome));
    }

    private static List<Map<String, Object>> results(final Map<String, Object> leaderboardBody) {
        @SuppressWarnings("unchecked")
        final var results = (List<Map<String, Object>>) leaderboardBody.get("results");
        return results == null ? List.of() : results;
    }

    private static int intValue(final Map<String, Object> body, final String field) {
        return Integer.parseInt(String.valueOf(body.get(field)).split("\\.")[0]);
    }

    private static List<Integer> ranksUpTo(final int size) {
        return rangeClosed(1, size).boxed().toList();
    }

    private static String uniqueName() {
        return "Duke-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
