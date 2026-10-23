package tech.virtua.guessthenumber.game;

/**
 * The result of one accepted guess: how it compared to the secret number, and
 * the status of the game once that guess was counted.
 */
public record GuessOutcome(int guess, Hint hint, GameStatus status) {
}
