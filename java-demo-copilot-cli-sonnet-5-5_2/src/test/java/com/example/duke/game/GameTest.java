package com.example.duke.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GameTest {

    private static final int SECRET = 6;

    private final Game game = new Game("game-1", SECRET);

    @Test
    void reportsTooLowAndTooHighWithoutEndingTheGame() {
        assertThat(game.guess(2)).isEqualTo(GuessOutcome.TOO_LOW);
        assertThat(game.guess(9)).isEqualTo(GuessOutcome.TOO_HIGH);

        final var snapshot = game.snapshot();
        assertThat(snapshot.status()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(snapshot.guessesMade()).isEqualTo(2);
        assertThat(snapshot.guessesRemaining()).isEqualTo(GameRules.MAX_GUESSES - 2);
        assertThat(snapshot.revealedSecret()).isEmpty();
    }

    @Test
    void winsOnCorrectGuessAndRejectsFurtherGuesses() {
        assertThat(game.guess(SECRET)).isEqualTo(GuessOutcome.CORRECT);

        assertThat(game.snapshot().status()).isEqualTo(GameStatus.WON);
        assertThat(game.snapshot().revealedSecret()).isEmpty();
        assertThatThrownBy(() -> game.guess(1)).isInstanceOf(GameFinishedException.class);
    }

    @Test
    void losesAfterFiveWrongGuessesAndRevealsTheSecret() {
        for (var attempt = 0; attempt < GameRules.MAX_GUESSES; attempt++) {
            game.guess(1);
        }

        final var snapshot = game.snapshot();
        assertThat(snapshot.status()).isEqualTo(GameStatus.LOST);
        assertThat(snapshot.guessesRemaining()).isZero();
        assertThat(snapshot.revealedSecret()).contains(SECRET);
        assertThatThrownBy(() -> game.guess(SECRET)).isInstanceOf(GameFinishedException.class);
        assertThat(game.snapshot().guessesRemaining()).isZero();
    }

    @Test
    void winningOnTheLastGuessIsAWin() {
        for (var attempt = 1; attempt < GameRules.MAX_GUESSES; attempt++) {
            game.guess(1);
        }
        game.guess(SECRET);

        assertThat(game.snapshot().status()).isEqualTo(GameStatus.WON);
        assertThat(game.snapshot().guessesMade()).isEqualTo(GameRules.MAX_GUESSES);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 11, 100})
    void outOfRangeGuessesAreRejectedAndNotCounted(final int value) {
        assertThatThrownBy(() -> game.guess(value)).isInstanceOf(InvalidGuessException.class);

        assertThat(game.snapshot().guessesMade()).isZero();
        assertThat(game.snapshot().guessesRemaining()).isEqualTo(GameRules.MAX_GUESSES);
    }

    @Test
    void recordsAWinExactlyOnceWithItsGuessCount() {
        game.guess(1);
        game.guess(SECRET);

        final int recorded = game.recordWin(guesses -> guesses);
        assertThat(recorded).isEqualTo(2);
        assertThatThrownBy(() -> game.recordWin(guesses -> guesses))
                .isInstanceOf(ScoreNotPostableException.class);
    }

    @Test
    void doesNotCountAFailedRecordingAsPosted() {
        game.guess(SECRET);

        assertThatThrownBy(() -> game.recordWin(_ -> {
            throw new IllegalStateException("database down");
        })).isInstanceOf(IllegalStateException.class);

        final int recorded = game.recordWin(guesses -> guesses);
        assertThat(recorded).isEqualTo(1);
    }

    @Test
    void refusesToRecordGamesThatWereNotWon() {
        assertThatThrownBy(() -> game.recordWin(guesses -> guesses))
                .isInstanceOf(ScoreNotPostableException.class);
    }
}
