package tech.virtua.duke;

/// The result of the player's most recent submission.
public enum GuessFeedback {

    /// Nothing has been submitted yet in this game.
    NONE,

    /// The submission was not a whole number in range, so it did not count.
    REJECTED,

    /// An accepted guess above the secret number.
    TOO_HIGH,

    /// An accepted guess below the secret number.
    TOO_LOW,

    /// An accepted guess equal to the secret number.
    CORRECT
}
