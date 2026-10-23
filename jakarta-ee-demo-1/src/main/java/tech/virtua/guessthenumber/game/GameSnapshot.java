package tech.virtua.guessthenumber.game;

/**
 * An immutable read of a game at one moment. Callers outside the session get a
 * snapshot rather than the live game, so state cannot shift under them mid-render.
 *
 * @param secretNumber the answer, revealed only once the game is over, otherwise {@code null}
 */
public record GameSnapshot(
        GameStatus status,
        int guessesMade,
        int guessesRemaining,
        Integer lastGuess,
        Hint lastHint,
        Integer secretNumber) {
}
