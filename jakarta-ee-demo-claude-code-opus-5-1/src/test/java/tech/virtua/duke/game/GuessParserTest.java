package tech.virtua.duke.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuessParserTest {

    @ParameterizedTest
    @ValueSource(strings = {"1", "10", "7", " 7 ", "+7", "07"})
    @DisplayName("reads whole numbers in range, however they are typed")
    void readsWholeNumbersInRange(final String rawGuess) {
        assertTrue(GuessParser.parse(rawGuess).isPresent(), rawGuess + " should be a guess");
    }

    @Test
    void readsTheNumberThatWasTyped() {
        assertEquals(7, GuessParser.parse(" 7 ").getAsInt());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "0", "11", "-1", "3.5", "seven", "7a", "1 2", "1e1",
            "٧", "2147483648", "99999999999999999999"})
    @DisplayName("turns down anything that is not a whole number from 1 to 10")
    void turnsDownEverythingElse(final String rawGuess) {
        assertTrue(GuessParser.parse(rawGuess).isEmpty(), rawGuess + " should not be a guess");
    }
}
