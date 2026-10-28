package com.example.guessgame.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for game session state transitions. No Spring context needed.
 */
class GameSessionStateTest {

    private GameSessionState state;

    @BeforeEach
    void setUp() {
        state = new GameSessionState();
    }

    @Test
    void newSession_isActive_withFiveGuessesLeft() {
        var snap = state.toSnapshot();
        assertThat(snap.state()).isEqualTo(GameState.ACTIVE);
        assertThat(snap.guessCount()).isZero();
        assertThat(snap.guessesLeft()).isEqualTo(GameSession.MAX_GUESSES);
    }

    @Test
    void invalidGuess_doesNotCountAgainstLimit() {
        state.evaluate(0);  // invalid: below range
        var snap = state.toSnapshot();
        assertThat(snap.outcome()).isEqualTo(GuessOutcome.INVALID);
        assertThat(snap.guessCount()).isZero();
        assertThat(snap.guessesLeft()).isEqualTo(GameSession.MAX_GUESSES);
    }

    @Test
    void invalidGuess_above10_doesNotCountAgainstLimit() {
        state.evaluate(11);
        var snap = state.toSnapshot();
        assertThat(snap.outcome()).isEqualTo(GuessOutcome.INVALID);
        assertThat(snap.guessCount()).isZero();
    }

    @Test
    void correctGuess_transitionsToWon() {
        // Force a known secret by peeking at the actual value via repeated guesses
        forceWin();
        var snap = state.toSnapshot();
        assertThat(snap.state()).isEqualTo(GameState.WON);
        assertThat(snap.outcome()).isEqualTo(GuessOutcome.CORRECT);
        assertThat(snap.secretNumber()).isNull(); // not revealed on win
    }

    @Test
    void exhaustingGuesses_transitionsToLost() {
        exhaustAllGuesses();
        var snap = state.toSnapshot();
        assertThat(snap.state()).isEqualTo(GameState.LOST);
        assertThat(snap.secretNumber()).isNotNull(); // revealed on loss
        assertThat(snap.guessCount()).isEqualTo(GameSession.MAX_GUESSES);
    }

    @Test
    void guessAfterLost_isIgnored() {
        exhaustAllGuesses();
        var before = state.toSnapshot();
        state.evaluate(5); // extra guess after game over
        var after = state.toSnapshot();
        assertThat(after.guessCount()).isEqualTo(before.guessCount());
        assertThat(after.state()).isEqualTo(GameState.LOST);
    }

    @Test
    void reset_clearsStateCompletely() {
        exhaustAllGuesses();
        state.reset();
        var snap = state.toSnapshot();
        assertThat(snap.state()).isEqualTo(GameState.ACTIVE);
        assertThat(snap.guessCount()).isZero();
        assertThat(snap.guessesLeft()).isEqualTo(GameSession.MAX_GUESSES);
    }

    @Test
    void feedbackForTooLow_mentionsTryHigher() {
        // Ensure the secret is > 1 so 1 is always too low; otherwise this is a win
        // Use reset until secret != 1
        ensureSecretIsNot(1);
        state.evaluate(1);
        assertThat(state.toSnapshot().feedback()).containsIgnoringCase("higher");
    }

    @Test
    void feedbackForTooHigh_mentionsTryLower() {
        ensureSecretIsNot(10);
        state.evaluate(10);
        assertThat(state.toSnapshot().feedback()).containsIgnoringCase("lower");
    }

    // --- helpers ---

    /** Evaluates wrong guesses until the guess limit is reached without winning. */
    private void exhaustAllGuesses() {
        var secret = state.getSecretNumber();
        // Pick a wrong value to guess repeatedly
        var wrong = (secret == 1) ? 2 : 1;
        for (int i = 0; i < GameSession.MAX_GUESSES; i++) {
            state.evaluate(wrong);
            if (state.getState() != GameState.ACTIVE) break;
        }
    }

    /** Forces a win by guessing the actual secret number. */
    private void forceWin() {
        state.evaluate(state.getSecretNumber());
    }

    /** Resets until the session's secret number is not the given value. Bounded to avoid infinite loops. */
    private void ensureSecretIsNot(int forbidden) {
        final int maxAttempts = 50;
        for (int attempt = 0; attempt < maxAttempts && state.getSecretNumber() == forbidden; attempt++) {
            state.reset();
        }
    }
}
