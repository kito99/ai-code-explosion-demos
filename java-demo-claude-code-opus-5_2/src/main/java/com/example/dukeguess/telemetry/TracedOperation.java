package com.example.dukeguess.telemetry;

/// The server-side operations that get their own span, named so traces read in game terms
/// rather than in HTTP terms.
public enum TracedOperation {

    START_GAME("duke.game.start"),
    SUBMIT_GUESS("duke.game.guess"),
    POST_SCORE("duke.game.post-score"),
    READ_LEADERBOARD("duke.leaderboard.read");

    private final String observationName;

    TracedOperation(final String observationName) {
        this.observationName = observationName;
    }

    public String observationName() {
        return observationName;
    }
}
