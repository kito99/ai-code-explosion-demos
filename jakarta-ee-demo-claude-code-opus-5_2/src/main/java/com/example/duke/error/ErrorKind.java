package com.example.duke.error;

/// The kinds of failure the application reports to a caller. Each one is mapped to an HTTP
/// status at the REST boundary, so nothing here depends on HTTP.
public enum ErrorKind {

    /// The caller sent something the rules do not allow, such as a guess outside 1 to 10.
    INVALID_INPUT,

    /// The session has no game yet, or the session expired.
    NO_GAME_IN_PROGRESS,

    /// The game already ended, so it accepts no further guesses.
    GAME_ALREADY_OVER,

    /// A score was requested for a session that has not won a game.
    NO_WINNING_GAME,

    /// The session already posted the score for its winning game.
    SCORE_ALREADY_POSTED,

    /// Anything the application did not anticipate.
    UNEXPECTED
}
