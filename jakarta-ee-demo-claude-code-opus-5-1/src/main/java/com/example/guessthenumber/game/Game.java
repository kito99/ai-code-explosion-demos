package com.example.guessthenumber.game;

import java.io.Serializable;
import java.util.OptionalInt;

import static java.util.Objects.requireNonNull;

/// A single round: a secret number, the guesses spent on it so far, and whether it is
/// still open. Lives in the player's session, so it is confined to one player.
public final class Game implements Serializable {

    private final GameRules rules;
    private final int secretNumber;
    private int guessesMade;
    private GameStatus status = GameStatus.ACTIVE;
    private boolean scorePosted;

    public Game(final GameRules rules, final int secretNumber) {
        this.rules = requireNonNull(rules, "rules");
        if (!rules.allows(secretNumber)) {
            throw new IllegalArgumentException(
                    "secretNumber %d is outside the rules %s".formatted(secretNumber, rules));
        }
        this.secretNumber = secretNumber;
    }

    /// Applies a guess and reports how it compared, moving the game to [GameStatus#WON]
    /// on a match and to [GameStatus#LOST] when the guess limit runs out.
    ///
    /// @throws GameAlreadyOverException if the game has already been won or lost
    /// @throws InvalidGuessException if the guess is outside the allowed range
    public GuessOutcome guess(final int guess) {
        if (status.isFinished()) {
            throw new GameAlreadyOverException();
        }
        if (!rules.allows(guess)) {
            throw new InvalidGuessException(rules);
        }

        guessesMade++;
        if (guess == secretNumber) {
            status = GameStatus.WON;
            return GuessOutcome.CORRECT;
        }
        if (guessesMade >= rules.maxGuesses()) {
            status = GameStatus.LOST;
        }
        return guess < secretNumber ? GuessOutcome.TOO_LOW : GuessOutcome.TOO_HIGH;
    }

    public GameRules rules() {
        return rules;
    }

    public GameStatus status() {
        return status;
    }

    public int guessesMade() {
        return guessesMade;
    }

    public int guessesRemaining() {
        return rules.maxGuesses() - guessesMade;
    }

    /// The secret number, but only once the game is over, so an active game cannot leak it.
    public OptionalInt revealedSecretNumber() {
        return status.isFinished() ? OptionalInt.of(secretNumber) : OptionalInt.empty();
    }

    public boolean isScorePosted() {
        return scorePosted;
    }

    /// Records that this win has reached the leaderboard, so a repeated submission cannot
    /// add a second entry for the same game.
    public void markScorePosted() {
        scorePosted = true;
    }
}
