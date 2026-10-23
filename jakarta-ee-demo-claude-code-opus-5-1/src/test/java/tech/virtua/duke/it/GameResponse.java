package tech.virtua.duke.it;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Objects;
import java.util.OptionalInt;

/// One answer from the game's API, read as the fields the page cares about.
record GameResponse(int statusCode, JsonObject body) {

    GameResponse {
        Objects.requireNonNull(body);
    }

    String status() {
        return body.getString("status");
    }

    String outcome() {
        return body.getString("outcome");
    }

    String feedback() {
        return body.getString("feedback");
    }

    String message() {
        return body.getString("message");
    }

    int guessesMade() {
        return body.getInt("guessesMade");
    }

    int guessesRemaining() {
        return body.getInt("guessesRemaining");
    }

    OptionalInt lastGuess() {
        return number("lastGuess");
    }

    OptionalInt secretNumber() {
        return number("secretNumber");
    }

    private OptionalInt number(final String field) {
        final var value = body.get(field);
        return value == null || value == JsonValue.NULL
                ? OptionalInt.empty()
                : OptionalInt.of(body.getInt(field));
    }
}
