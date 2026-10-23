package tech.virtua.guessthenumber.game;

/** Where a game currently stands. */
public enum GameStatus {
    /** The player still has guesses left and has not found the secret number. */
    IN_PROGRESS,
    /** The player guessed the secret number. */
    WON,
    /** The player used every guess without finding the secret number. */
    LOST
}
