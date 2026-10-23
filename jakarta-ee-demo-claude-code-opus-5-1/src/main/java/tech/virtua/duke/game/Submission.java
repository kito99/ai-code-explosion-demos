package tech.virtua.duke.game;

import java.util.Objects;

/// What one submission did: how the game treated it, and the game it left behind.
public record Submission(SubmissionOutcome outcome, GuessingGame game) {

    public Submission {
        Objects.requireNonNull(outcome);
        Objects.requireNonNull(game);
    }

    /// The resulting game as the player may see it.
    public GameSnapshot snapshot() {
        return game.snapshot();
    }
}
