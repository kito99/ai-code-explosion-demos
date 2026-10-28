package com.example.duke.leaderboard;

import com.example.duke.api.validation.PlayerNameValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.example.duke.leaderboard.PlayerName.MAX_LENGTH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The Jakarta Validation constraint must agree with the name rules exactly, so a name is
/// never turned away at one layer and accepted at another.
class PlayerNameValidatorTest {

    private final PlayerNameValidator validator = new PlayerNameValidator();

    @Test
    @DisplayName("the constraint accepts what the name rules accept")
    void acceptsAcceptableNames() {
        assertTrue(validator.isValid("Duke", null));
        assertTrue(validator.isValid("  Duke  ", null));
        assertTrue(validator.isValid("D".repeat(MAX_LENGTH), null));
    }

    @Test
    @DisplayName("the constraint rejects what the name rules reject")
    void rejectsUnacceptableNames() {
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("   ", null));
        assertFalse(validator.isValid("D".repeat(MAX_LENGTH + 1), null));
    }
}
