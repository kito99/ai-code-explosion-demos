package com.example.dukeguess.leaderboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PlayerNameTest {

    @Test
    void keepsAPlainName() {
        assertThat(PlayerName.of("Duke").value()).isEqualTo("Duke");
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(PlayerName.of("  Duke \n").value()).isEqualTo("Duke");
    }

    @Test
    void acceptsAFullLengthName() {
        final var thirtyCharacters = "x".repeat(PlayerName.MAX_LENGTH);

        assertThat(PlayerName.of(thirtyCharacters).value()).isEqualTo(thirtyCharacters);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void turnsAwayANameWithoutAVisibleCharacter(final String blank) {
        assertThatExceptionOfType(InvalidPlayerNameException.class)
                .isThrownBy(() -> PlayerName.of(blank));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u200b", "\u200b\u200b", "\u00a0", "\ufeff"})
    void turnsAwayANameBuiltOnlyFromInvisibleCharacters(final String invisible) {
        assertThatExceptionOfType(InvalidPlayerNameException.class)
                .isThrownBy(() -> PlayerName.of(invisible));
    }

    @Test
    void measuresEmojiAsTheCharactersTheyLookLike() {
        final var thirtyEmoji = "\ud83d\ude00".repeat(PlayerName.MAX_LENGTH);

        assertThat(PlayerName.of(thirtyEmoji).value()).isEqualTo(thirtyEmoji);
        assertThatExceptionOfType(InvalidPlayerNameException.class)
                .isThrownBy(() -> PlayerName.of("\ud83d\ude00".repeat(PlayerName.MAX_LENGTH + 1)));
    }

    @Test
    void turnsAwayANameLongerThanTheLimit() {
        final var tooLong = "x".repeat(PlayerName.MAX_LENGTH + 1);

        assertThatExceptionOfType(InvalidPlayerNameException.class)
                .isThrownBy(() -> PlayerName.of(tooLong));
    }

    @Test
    void measuresLengthAfterTrimming() {
        final var paddedToOverTheLimit = "  " + "x".repeat(PlayerName.MAX_LENGTH) + "  ";

        assertThat(PlayerName.of(paddedToOverTheLimit).value()).hasSize(PlayerName.MAX_LENGTH);
    }
}
