package tech.virtua.guessthenumber.game;

/** What the game did with a submitted guess. */
public enum Acceptance {
    /** The guess was valid, counted against the limit, and produced feedback. */
    ACCEPTED,
    /** The input was not a whole number from 1 to 10, so it did not count. */
    INVALID_INPUT,
    /** The game was already won or lost, so no further guesses are taken. */
    GAME_OVER
}
