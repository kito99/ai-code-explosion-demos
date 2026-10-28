package com.example.guessthenumber.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRulesTest {

    @Test
    void theStandardRulesAreOneToTenInFiveGuesses() {
        assertEquals(new GameRules(1, 10, 5), GameRules.STANDARD);
    }

    @Test
    void acceptsOnlyNumbersInsideTheRange() {
        assertTrue(GameRules.STANDARD.allows(1));
        assertTrue(GameRules.STANDARD.allows(10));
        assertFalse(GameRules.STANDARD.allows(0));
        assertFalse(GameRules.STANDARD.allows(11));
    }

    @Test
    void refusesAnEmptyRangeOrNoGuesses() {
        assertThrows(IllegalArgumentException.class, () -> new GameRules(10, 1, 5));
        assertThrows(IllegalArgumentException.class, () -> new GameRules(1, 10, 0));
    }
}
