package tech.virtua.duke;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.virtua.duke.GameRules.HIGHEST_NUMBER;
import static tech.virtua.duke.GameRules.LOWEST_NUMBER;
import static tech.virtua.duke.GameStatus.WON;
import static tech.virtua.duke.GuessFeedback.TOO_HIGH;

/// Plays the real game, secret and all, by following the high and low feedback.
/// A halving search always lands within the guess limit, so if the feedback is
/// ever wrong this test loses.
class RandomGameEndToEndTest {

    private static final int EPHEMERAL_PORT = 0;
    private static final int GAMES_PLAYED = 25;

    private GameServer server;
    private ApiClient player;

    @BeforeEach
    void startServer() throws IOException {
        server = GameServer.start(EPHEMERAL_PORT, SecretNumberPicker.random());
        player = new ApiClient(server.port());
    }

    @AfterEach
    void stopServer() {
        player.close();
        server.close();
    }

    @RepeatedTest(GAMES_PLAYED)
    void findsTheSecretByFollowingTheFeedback() throws Exception {
        player.newGame();

        var lowest = LOWEST_NUMBER;
        var highest = HIGHEST_NUMBER;
        var response = player.currentGame();

        while (!WON.name().equals(response.text("status")) && lowest <= highest) {
            final var guess = (lowest + highest) / 2;
            response = player.guess(Integer.toString(guess));
            if (TOO_HIGH.name().equals(response.text("feedback"))) {
                highest = guess - 1;
            } else {
                lowest = guess + 1;
            }
        }

        final var outcome = response;
        assertEquals(WON.name(), outcome.text("status"), () -> "lost with: " + outcome.text("message"));
        final var secret = outcome.number("secretNumber");
        assertTrue(GameRules.isInRange(secret), () -> "secret out of range: " + secret);
        assertEquals(secret, outcome.number("lastGuess"));
    }
}
