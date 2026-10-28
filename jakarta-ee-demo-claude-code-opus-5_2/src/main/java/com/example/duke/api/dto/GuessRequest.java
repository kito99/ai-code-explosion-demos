package com.example.duke.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

/// A guess at the secret number.
///
/// Jakarta Validation checks the shape here — that a guess arrived and that it is a whole
/// number — while whether it falls in the allowed range is the game's own rule, checked by
/// [com.example.duke.game.Game]. The value is read as a decimal so a fractional guess is
/// caught rather than quietly rounded to the nearest whole number.
///
/// The range in the schema below is written out because an annotation takes only constants;
/// it mirrors [com.example.duke.game.GameRules].
@Schema(name = "GuessRequest", description = "A guess at Duke's secret number")
public record GuessRequest(

        @NotNull(message = SHAPE_MESSAGE)
        @Digits(integer = GUESS_DIGITS, fraction = 0, message = SHAPE_MESSAGE)
        @Schema(type = SchemaType.INTEGER, description = "A whole number from 1 to 10",
                minimum = "1", maximum = "10")
        BigDecimal guess) {

    /// Every guess the game allows fits in two digits, so a longer one is turned away as the
    /// wrong shape before the range is ever considered.
    static final int GUESS_DIGITS = 2;

    static final String SHAPE_MESSAGE = "Enter a whole number from 1 to 10.";

    /// The guess as a whole number. This is safe because validation has already turned away
    /// anything with a fractional part or more digits than [#GUESS_DIGITS].
    public int value() {
        return guess.intValueExact();
    }
}
