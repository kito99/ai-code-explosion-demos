package com.example.duke.game;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final int SECRET = 6;

    private final Game game = new Game(SECRET, NOW);

    @Test
    void givesHighAndLowFeedbackAndCountsGuesses() {
        assertThat(game.guess(9, NOW).outcome()).isEqualTo(GuessOutcome.TOO_HIGH);
        final var result = game.guess(2, NOW);

        assertThat(result.outcome()).isEqualTo(GuessOutcome.TOO_LOW);
        assertThat(result.game().guessesMade()).isEqualTo(2);
        assertThat(result.game().guessesRemaining()).isEqualTo(3);
        assertThat(result.game().revealedSecret()).isEmpty();
    }

    @Test
    void winsOnCorrectGuessAndRefusesFurtherGuesses() {
        final var result = game.guess(SECRET, NOW);

        assertThat(result.game().status()).isEqualTo(GameStatus.WON);
        assertThatThrownBy(() -> game.guess(1, NOW)).isInstanceOf(GameException.class)
                .extracting("error").isEqualTo(GameError.GAME_FINISHED);
    }

    @Test
    void losesAfterFiveWrongGuessesAndRevealsSecret() {
        for (int attempt = 0; attempt < GameRules.MAX_GUESSES; attempt++) {
            game.guess(1, NOW);
        }

        final var snapshot = game.snapshot();
        assertThat(snapshot.status()).isEqualTo(GameStatus.LOST);
        assertThat(snapshot.guessesRemaining()).isZero();
        assertThat(snapshot.revealedSecret()).contains(SECRET);
    }

    @Test
    void outOfRangeGuessIsNotCounted() {
        assertThatThrownBy(() -> game.guess(0, NOW)).isInstanceOf(GameException.class);
        assertThatThrownBy(() -> game.guess(11, NOW)).isInstanceOf(GameException.class);

        assertThat(game.snapshot().guessesMade()).isZero();
        assertThat(game.snapshot().guessesRemaining()).isEqualTo(GameRules.MAX_GUESSES);
    }

    @Test
    void scoreCanOnlyBeClaimedOnceAndOnlyAfterAWin() {
        assertThatThrownBy(() -> game.claimScore(NOW)).extracting("error").isEqualTo(GameError.GAME_NOT_WON);

        game.guess(SECRET, NOW);
        assertThat(game.claimScore(NOW)).isEqualTo(1);
        assertThatThrownBy(() -> game.claimScore(NOW)).extracting("error").isEqualTo(GameError.SCORE_ALREADY_POSTED);
    }
}
