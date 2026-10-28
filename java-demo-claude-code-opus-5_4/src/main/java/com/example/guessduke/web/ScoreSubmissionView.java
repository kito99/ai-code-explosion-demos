package com.example.guessduke.web;

/// The outcome of trying to post a win to the leaderboard, with the text to show
/// the player either way.
public record ScoreSubmissionView(boolean posted, String message) {

    /// Reports a win that reached the leaderboard.
    public static ScoreSubmissionView accepted(final String message) {
        return new ScoreSubmissionView(true, message);
    }

    /// Reports a submission that recorded nothing, and why.
    public static ScoreSubmissionView declined(final String message) {
        return new ScoreSubmissionView(false, message);
    }
}
