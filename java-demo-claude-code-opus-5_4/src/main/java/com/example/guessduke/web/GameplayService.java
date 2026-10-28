package com.example.guessduke.web;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.example.guessduke.game.GuessOutcome;
import com.example.guessduke.game.GuessParser;
import com.example.guessduke.game.GuessingGame;
import com.example.guessduke.game.SecretNumberGenerator;

import static com.example.guessduke.game.GameStatus.LOST;
import static com.example.guessduke.game.GuessOutcome.CORRECT;
import static com.example.guessduke.web.GameMessages.GUESS_NOT_ACCEPTED;
import static com.example.guessduke.web.GameMessages.NEW_GAME;
import static com.example.guessduke.web.GameMessages.ROUND_ALREADY_OVER;
import static com.example.guessduke.web.GameMessages.SESSION_RESTARTED;

/// Runs a round from the player's point of view: starts games, decides which
/// guesses count, and pairs every result with the text that explains it.
@Service
public class GameplayService {

    private final SecretNumberGenerator secretNumberGenerator;

    public GameplayService(final SecretNumberGenerator secretNumberGenerator) {
        this.secretNumberGenerator = Objects.requireNonNull(secretNumberGenerator, "secretNumberGenerator");
    }

    /// Starts a round on a freshly drawn secret number.
    public GuessingGame newGame() {
        return new GuessingGame(secretNumberGenerator.nextSecretNumber());
    }

    /// Presents a round the player just asked to start.
    public GameView welcome(final GuessingGame game) {
        return GameView.of(game, NEW_GAME);
    }

    /// Presents a round started in place of one the player's session had lost.
    public GameView afterLostSession(final GuessingGame game) {
        return GameView.of(game, SESSION_RESTARTED);
    }

    /// Scores a guess the player typed. Input that is not a playable whole number,
    /// and any guess arriving after the round ended, changes nothing.
    public GameView submitGuess(final GuessingGame game, final String rawGuess) {
        if (game.status().isFinished()) {
            return GameView.of(game, ROUND_ALREADY_OVER);
        }
        return GuessParser.parse(rawGuess)
                .map(guess -> scoreGuess(game, guess))
                .orElseGet(() -> GameView.of(game, GUESS_NOT_ACCEPTED));
    }

    private static GameView scoreGuess(final GuessingGame game, final int guess) {
        final var outcome = game.submitGuess(guess);
        return GameView.of(game, describe(game, guess, outcome));
    }

    private static String describe(final GuessingGame game, final int guess, final GuessOutcome outcome) {
        if (outcome == CORRECT) {
            return GameMessages.won(game.secretNumber(), game.guessesMade());
        }
        return game.status() == LOST
                ? GameMessages.lost(guess, outcome, game.secretNumber())
                : GameMessages.missed(guess, outcome, game.guessesRemaining());
    }
}
