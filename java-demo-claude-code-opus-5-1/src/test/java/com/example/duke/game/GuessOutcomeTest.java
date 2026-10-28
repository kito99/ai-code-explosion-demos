package com.example.duke.game;

import org.junit.jupiter.api.Test;

import static com.example.duke.game.GuessOutcome.CORRECT;
import static com.example.duke.game.GuessOutcome.TOO_HIGH;
import static com.example.duke.game.GuessOutcome.TOO_LOW;
import static org.assertj.core.api.Assertions.assertThat;

class GuessOutcomeTest {

    @Test
    void comparesGuessAgainstSecret() {
        assertThat(GuessOutcome.of(3, 7)).isEqualTo(TOO_LOW);
        assertThat(GuessOutcome.of(9, 7)).isEqualTo(TOO_HIGH);
        assertThat(GuessOutcome.of(7, 7)).isEqualTo(CORRECT);
    }
}
