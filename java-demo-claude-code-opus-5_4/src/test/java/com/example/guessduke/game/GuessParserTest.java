package com.example.guessduke.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class GuessParserTest {

    @ParameterizedTest(name = "\"{0}\" reads as a playable number")
    @ValueSource(strings = {"1", "10", " 7 ", "+4"})
    void acceptsPlayableWholeNumbers(final String rawGuess) {
        assertThat(GuessParser.parse(rawGuess)).isPresent();
    }

    @Test
    void readsTheNumberThePlayerTyped() {
        assertThat(GuessParser.parse(" 8 ")).contains(8);
    }

    @ParameterizedTest(name = "\"{0}\" is not a playable guess")
    @ValueSource(strings = {"", "   ", "abc", "3.5", "0", "11", "-2", "7a"})
    @NullSource
    void rejectsAnythingElse(final String rawGuess) {
        assertThat(GuessParser.parse(rawGuess)).isEmpty();
    }
}
