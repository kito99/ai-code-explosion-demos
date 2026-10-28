package com.example.guessgame.game;

/**
 * Immutable snapshot of a game session, safe to expose to the view layer.
 *
 * @param state          current game state
 * @param guessCount     number of accepted guesses made so far
 * @param guessesLeft    number of accepted guesses still available
 * @param outcome        outcome of the most recent guess (null on game start)
 * @param secretNumber   revealed only in the lost state; null otherwise
 * @param feedback       human-readable feedback message for the last guess
 */
public record GameSession(
        GameState state,
        int guessCount,
        int guessesLeft,
        GuessOutcome outcome,
        Integer secretNumber,
        String feedback
) {
    /** Maximum guesses allowed per game (FR-3). */
    public static final int MAX_GUESSES = 5;
}
