package com.example.guessduke.game;

import java.util.Optional;

import static com.example.guessduke.game.GameRules.isPlayableNumber;

/// Turns whatever the player typed into a playable number, or nothing at all.
public final class GuessParser {

    private GuessParser() {
    }

    /// Reads a playable whole number from raw player input, returning empty when the
    /// text is blank, is not a whole number, or falls outside the playable range.
    public static Optional<Integer> parse(final String rawGuess) {
        if (rawGuess == null || rawGuess.isBlank()) {
            return Optional.empty();
        }
        try {
            final var guess = Integer.parseInt(rawGuess.trim());
            return isPlayableNumber(guess) ? Optional.of(guess) : Optional.empty();
        } catch (final NumberFormatException _) {
            return Optional.empty();
        }
    }
}
