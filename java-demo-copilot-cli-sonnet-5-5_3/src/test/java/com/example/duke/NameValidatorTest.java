package com.example.duke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class NameValidatorTest {

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals("Duke", NameValidator.validate("  Duke \t"));
    }

    @Test
    void acceptsExactlyThirtyCharacters() {
        final var name = "a".repeat(GameRules.MAX_NAME_LENGTH);
        assertEquals(name, NameValidator.validate(name));
    }

    @Test
    void rejectsThirtyOneCharacters() {
        final var name = "a".repeat(GameRules.MAX_NAME_LENGTH + 1);
        assertThrows(InvalidNameException.class, () -> NameValidator.validate(name));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t\n"})
    void rejectsBlankNames(final String name) {
        assertThrows(InvalidNameException.class, () -> NameValidator.validate(name));
    }
}
