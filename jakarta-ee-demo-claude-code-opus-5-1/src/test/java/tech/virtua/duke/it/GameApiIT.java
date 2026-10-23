package tech.virtua.duke.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Plays whole games against a running server.
class GameApiIT {

    private static final int LOWEST_NUMBER = 1;
    private static final int HIGHEST_NUMBER = 10;
    private static final int GUESS_LIMIT = 5;

    /// A guess of 1 is wrong in nine games out of ten, but the tenth is a win on the
    /// opening guess, which is no use to a test about losing. Since the server draws its
    /// own number, the only way to reach the lose state is to deal again when that
    /// happens. Twenty deals makes giving up less likely than the hardware failing.
    private static final int DEALS_BEFORE_GIVING_UP = 20;

    private final GameClient player = new GameClient();

    @Test
    void dealsAGameToABrowserThatHasNotPlayedYet() {
        final var game = player.currentGame();
        assertAll(
                () -> assertEquals(200, game.statusCode()),
                () -> assertEquals("ACTIVE", game.status()),
                () -> assertEquals("NONE", game.outcome()),
                () -> assertEquals(0, game.guessesMade()),
                () -> assertEquals(GUESS_LIMIT, game.guessesRemaining()),
                () -> assertTrue(game.secretNumber().isEmpty(), "the secret number is not for the player"),
                () -> assertFalse(game.message().isBlank(), "every answer tells the player something"));
    }

    @Test
    void remembersTheGameFromOneRequestToTheNext() {
        player.newGame();
        player.guess("1");
        assertEquals(1, player.currentGame().guessesMade());
    }

    @Test
    void givesEachBrowserItsOwnGame() {
        final var otherPlayer = new GameClient();
        player.newGame();
        otherPlayer.newGame();
        player.guess("1");
        assertAll(
                () -> assertEquals(1, player.currentGame().guessesMade()),
                () -> assertEquals(0, otherPlayer.currentGame().guessesMade()));
    }

    @Test
    @DisplayName("a player who follows the high and low feedback always wins")
    void winsByFollowingTheFeedback() {
        final var won = playUntilWon();
        assertAll(
                () -> assertEquals(200, won.statusCode()),
                () -> assertEquals("WON", won.status()),
                () -> assertEquals("CORRECT", won.feedback()),
                () -> assertEquals(won.lastGuess().getAsInt(), won.secretNumber().getAsInt()),
                () -> assertTrue(won.message().contains(String.valueOf(won.secretNumber().getAsInt())),
                        "the win message names the number"),
                () -> assertTrue(won.message().contains(String.valueOf(won.guessesMade())),
                        "the win message counts the guesses"));
    }

    @Test
    void endsTheGameAndRevealsTheNumberAfterFiveWrongGuesses() {
        final var lost = playUntilLost();
        assertAll(
                () -> assertEquals(200, lost.statusCode()),
                () -> assertEquals("LOST", lost.status()),
                () -> assertEquals(GUESS_LIMIT, lost.guessesMade()),
                () -> assertEquals(0, lost.guessesRemaining()),
                () -> assertTrue(lost.secretNumber().isPresent(), "a lost game gives up its number"),
                () -> assertNotEquals(LOWEST_NUMBER, lost.secretNumber().getAsInt(),
                        "the number guessed five times over was wrong every time"),
                () -> assertTrue(lost.message().contains(String.valueOf(lost.secretNumber().getAsInt()))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "11", "-3", "3.5", "seven", "", "   "})
    @DisplayName("a submission that is not a whole number in range costs the player nothing")
    void turnsDownSubmissionsOutsideTheRules(final String rawGuess) {
        openWithOneWrongGuess();
        final var rejected = player.guess(rawGuess);
        assertAll(
                () -> assertEquals(400, rejected.statusCode()),
                () -> assertEquals("NOT_A_VALID_NUMBER", rejected.outcome()),
                () -> assertEquals("ACTIVE", rejected.status()),
                () -> assertEquals(1, rejected.guessesMade()),
                () -> assertEquals(GUESS_LIMIT - 1, rejected.guessesRemaining()),
                () -> assertTrue(rejected.message().contains("did not count")));
    }

    @Test
    void answersABodyItCannotReadWithTheSameFriendlyRejection() {
        player.newGame();
        final var rejected = player.submit("{\"guess\": ");
        assertAll(
                () -> assertEquals(400, rejected.statusCode()),
                () -> assertEquals("NOT_A_VALID_NUMBER", rejected.outcome()),
                () -> assertEquals(0, rejected.guessesMade()));
    }

    @Test
    void takesNoMoreGuessesOnceTheGameIsWon() {
        playUntilWon();
        final var tooLate = player.guess("1");
        assertAll(
                () -> assertEquals(409, tooLate.statusCode()),
                () -> assertEquals("GAME_ALREADY_OVER", tooLate.outcome()),
                () -> assertEquals("WON", tooLate.status()),
                () -> assertTrue(tooLate.message().contains("new game")));
    }

    @Test
    void startsOverWheneverThePlayerAsks() {
        playUntilWon();
        final var restarted = player.newGame();
        assertAll(
                () -> assertEquals(200, restarted.statusCode()),
                () -> assertEquals("ACTIVE", restarted.status()),
                () -> assertEquals("NONE", restarted.feedback()),
                () -> assertEquals(0, restarted.guessesMade()),
                () -> assertEquals(GUESS_LIMIT, restarted.guessesRemaining()),
                () -> assertTrue(restarted.lastGuess().isEmpty()),
                () -> assertTrue(restarted.secretNumber().isEmpty()));
    }

    @Test
    void keepsTheSecretNumberBackForAsLongAsTheGameRuns() {
        final var dealt = player.newGame();
        final var oneGuessIn = openWithOneWrongGuess();
        final var readBack = player.currentGame();
        assertAll(
                () -> assertTrue(dealt.secretNumber().isEmpty(), "a fresh game keeps its number"),
                () -> assertTrue(oneGuessIn.secretNumber().isEmpty(), "a game in play keeps its number"),
                () -> assertTrue(readBack.secretNumber().isEmpty(), "reading a game does not give it away"),
                () -> assertEquals("ACTIVE", readBack.status(), "the game is still being played"));
    }

    @Test
    void carriesTheRulesSoThePageNeverHasToKnowThem() {
        final var game = player.newGame();
        assertAll(
                () -> assertEquals(GUESS_LIMIT, game.body().getInt("guessLimit")),
                () -> assertEquals(LOWEST_NUMBER, game.body().getInt("lowestNumber")),
                () -> assertEquals(HIGHEST_NUMBER, game.body().getInt("highestNumber")));
    }

    /// Halves the range from the high and low feedback, which can only ever win if that
    /// feedback is right.
    private GameResponse playUntilWon() {
        player.newGame();
        var lowest = LOWEST_NUMBER;
        var highest = HIGHEST_NUMBER;
        while (lowest <= highest) {
            final var answer = player.guess(String.valueOf((lowest + highest) / 2));
            switch (answer.feedback()) {
                case "CORRECT" -> {
                    return answer;
                }
                case "TOO_LOW" -> lowest = answer.lastGuess().getAsInt() + 1;
                case "TOO_HIGH" -> highest = answer.lastGuess().getAsInt() - 1;
                default -> fail("Unexpected feedback: " + answer.feedback());
            }
        }
        return fail("Halving the range never found the number.");
    }

    private GameResponse playUntilLost() {
        var answer = openWithOneWrongGuess();
        while (answer.status().equals("ACTIVE")) {
            answer = player.guess(String.valueOf(LOWEST_NUMBER));
        }
        return answer;
    }

    /// Deals until the opening guess misses, which leaves a game one guess in and still
    /// being played.
    private GameResponse openWithOneWrongGuess() {
        for (var deal = 0; deal < DEALS_BEFORE_GIVING_UP; deal++) {
            player.newGame();
            final var answer = player.guess(String.valueOf(LOWEST_NUMBER));
            if (answer.status().equals("ACTIVE")) {
                return answer;
            }
        }
        return fail("Every one of %d games was won on the opening guess."
                .formatted(DEALS_BEFORE_GIVING_UP));
    }
}
