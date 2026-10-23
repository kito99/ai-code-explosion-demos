package tech.virtua.duke.game;

/// What the game did with a submission.
public enum SubmissionOutcome {

    /// Nothing was submitted; the game was only read.
    NONE,

    /// The submission was a legal guess and used one of the player's guesses.
    COUNTED,

    /// The submission was not a whole number in range, so it changed nothing.
    NOT_A_VALID_NUMBER,

    /// The game had already finished, so it changed nothing.
    GAME_ALREADY_OVER
}
