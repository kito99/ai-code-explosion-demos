package com.example.duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import com.example.duke.Dtos.GuessRequest;
import com.example.duke.Dtos.ScoreRequest;

import jakarta.validation.Validation;

class GameTest {

    private static Game gameWithSecret(int secret) {
        return new Game(secret);
    }

    @Test
    void wrongGuessesGiveHintsAndLoseAfterFiveGuesses() {
        Game game = gameWithSecret(5);
        assertEquals(Game.Hint.TOO_LOW, game.guess(2));
        assertEquals(Game.Hint.TOO_HIGH, game.guess(9));
        assertEquals(3, game.guessesRemaining());
        assertNull(game.revealedSecret());
        game.guess(1);
        game.guess(10);
        assertEquals(Game.Status.ACTIVE, game.status());
        game.guess(3);
        assertEquals(Game.Status.LOST, game.status());
        assertEquals(0, game.guessesRemaining());
        assertEquals(5, game.revealedSecret());
        assertEquals(409, assertThrows(GameException.class, () -> game.guess(5)).status());
    }

    @Test
    void correctGuessWinsAndStopsFurtherGuesses() {
        Game game = gameWithSecret(7);
        game.guess(1);
        assertEquals(Game.Hint.CORRECT, game.guess(7));
        assertEquals(Game.Status.WON, game.status());
        assertEquals(2, game.guessesMade());
        assertNull(game.revealedSecret());
        assertThrows(GameException.class, () -> game.guess(7));
    }

    @Test
    void scoreCanOnlyBePostedOnceForAWonGame() {
        Game lost = gameWithSecret(10);
        assertEquals(409, assertThrows(GameException.class, lost::claimScorePosting).status());

        Game won = gameWithSecret(4);
        won.guess(4);
        assertTrue(won.claimScorePosting());
        assertThrows(GameException.class, won::claimScorePosting);
    }

    @Test
    void secretNumbersStayWithinRangeAndCoverEveryValue() {
        Set<Integer> drawn = new HashSet<>();
        RandomGenerator recording = new RandomGenerator() {
            private final RandomGenerator delegate = RandomGenerator.getDefault();

            @Override
            public long nextLong() {
                return delegate.nextLong();
            }

            @Override
            public int nextInt(int origin, int bound) {
                int value = delegate.nextInt(origin, bound);
                drawn.add(value);
                return value;
            }
        };
        GameService service = new GameService(recording);
        for (int i = 0; i < 2000; i++) {
            service.start();
        }
        assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), drawn);
    }

    @Test
    void unknownGameIsNotFound() {
        GameService service = new GameService(RandomGenerator.getDefault());
        assertEquals(404, assertThrows(GameException.class, () -> service.find("nope")).status());
    }

    @Test
    void guessAndNameValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String ok : new String[] {"1", "10", " 7 "}) {
                assertTrue(validator.validate(new GuessRequest(ok)).isEmpty(), ok);
            }
            for (String bad : new String[] {"0", "11", "abc", "3.5", "", "-1", null}) {
                assertEquals(1, validator.validate(new GuessRequest(bad)).size(), String.valueOf(bad));
            }
            assertTrue(validator.validate(new ScoreRequest("  Duke  ")).isEmpty());
            assertEquals("Duke", new ScoreRequest("  Duke  ").name());
            assertTrue(validator.validate(new ScoreRequest("x".repeat(30))).isEmpty());
            assertEquals(1, validator.validate(new ScoreRequest("x".repeat(31))).size());
            assertEquals(1, validator.validate(new ScoreRequest("   ")).size());
            assertEquals(1, validator.validate(new ScoreRequest(null)).size());
        }
    }
}
