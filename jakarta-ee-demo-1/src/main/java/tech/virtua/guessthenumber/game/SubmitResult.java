package tech.virtua.guessthenumber.game;

/**
 * The outcome of submitting raw player input, paired with the game state that
 * resulted. On a rejected submission the snapshot is the unchanged game.
 */
public record SubmitResult(Acceptance acceptance, GameSnapshot game) {
}
