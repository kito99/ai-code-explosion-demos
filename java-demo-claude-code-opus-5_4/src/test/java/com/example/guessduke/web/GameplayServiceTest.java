package com.example.guessduke.web;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.guessduke.game.GuessingGame;

import static com.example.guessduke.game.GameRules.MAX_GUESSES;
import static com.example.guessduke.game.GameStatus.ACTIVE;
import static com.example.guessduke.game.GameStatus.LOST;
import static com.example.guessduke.game.GameStatus.WON;
import static org.assertj.core.api.Assertions.assertThat;

class GameplayServiceTest {

    private static final int SECRET = 6;

    private final GameplayService gameplayService = new GameplayService(() -> SECRET);

    @Test
    void welcomesThePlayerIntoAFreshRound() {
        final var view = gameplayService.welcome(gameplayService.newGame());

        assertThat(view.status()).isEqualTo(ACTIVE);
        assertThat(view.guessesMade()).isZero();
        assertThat(view.guessesRemaining()).isEqualTo(MAX_GUESSES);
        assertThat(view.message()).isEqualTo(GameMessages.NEW_GAME);
        assertThat(view.secretNumber()).isEmpty();
    }

    @Test
    void tellsThePlayerToAimHigher() {
        final var view = gameplayService.submitGuess(gameplayService.newGame(), "2");

        assertThat(view.message()).contains("too low").contains("higher");
        assertThat(view.guessesMade()).isEqualTo(1);
    }

    @Test
    void tellsThePlayerToAimLower() {
        final var view = gameplayService.submitGuess(gameplayService.newGame(), "9");

        assertThat(view.message()).contains("too high").contains("lower");
    }

    @ParameterizedTest(name = "\"{0}\" is rejected without costing a guess")
    @ValueSource(strings = {"0", "11", "abc", "", "   ", "4.5"})
    void rejectedInputChangesNothing(final String rawGuess) {
        final var game = gameplayService.newGame();

        final var view = gameplayService.submitGuess(game, rawGuess);

        assertThat(view.message()).isEqualTo(GameMessages.GUESS_NOT_ACCEPTED);
        assertThat(view.guessesMade()).isZero();
        assertThat(view.guessesRemaining()).isEqualTo(MAX_GUESSES);
        assertThat(view.status()).isEqualTo(ACTIVE);
    }

    @Test
    void congratulatesAWinAndReportsTheGuessesItTook() {
        final var game = gameplayService.newGame();
        gameplayService.submitGuess(game, "1");

        final var view = gameplayService.submitGuess(game, String.valueOf(SECRET));

        assertThat(view.status()).isEqualTo(WON);
        assertThat(view.message()).contains("You got it!").contains("2 guesses");
        assertThat(view.scorePosted()).isFalse();
    }

    @Test
    void revealsTheSecretNumberWhenTheLastGuessMisses() {
        final var game = gameplayService.newGame();
        GameView view = null;
        for (var attempt = 0; attempt < MAX_GUESSES; attempt++) {
            view = gameplayService.submitGuess(game, "10");
        }

        assertThat(view).isNotNull();
        assertThat(view.status()).isEqualTo(LOST);
        assertThat(view.guessesRemaining()).isZero();
        assertThat(view.secretNumber()).isEqualTo(Optional.of(SECRET));
        assertThat(view.message()).contains("last guess").contains(String.valueOf(SECRET));
    }

    @Test
    void anActiveRoundNeverRevealsTheSecretNumber() {
        final var view = gameplayService.submitGuess(gameplayService.newGame(), "3");

        assertThat(view.secretNumber()).isEmpty();
    }

    @Test
    void guessesArrivingAfterTheRoundEndedChangeNothing() {
        final var game = gameplayService.newGame();
        gameplayService.submitGuess(game, String.valueOf(SECRET));

        final var view = gameplayService.submitGuess(game, "3");

        assertThat(view.message()).isEqualTo(GameMessages.ROUND_ALREADY_OVER);
        assertThat(view.guessesMade()).isEqualTo(1);
        assertThat(view.status()).isEqualTo(WON);
    }

    @Test
    void aRestartedSessionGetsAFreshRoundAndKeepsTheGuessFree() {
        final var view = gameplayService.afterLostSession(gameplayService.newGame());

        assertThat(view.message()).isEqualTo(GameMessages.SESSION_RESTARTED);
        assertThat(view.guessesMade()).isZero();
    }

    @Test
    void drawsTheSecretNumberFromTheGenerator() {
        assertThat(new GuessingGame(SECRET).secretNumber()).isEqualTo(gameplayService.newGame().secretNumber());
    }
}
