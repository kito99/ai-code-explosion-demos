package tech.virtua.duke;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tech.virtua.duke.GameStatus.WON;

class JsonWriterTest {

    @Test
    void writesAnEmptyObject() {
        assertEquals("{}", JsonWriter.object().build());
    }

    @Test
    void keepsFieldsInInsertionOrder() {
        final var json = JsonWriter.object()
                .put("first", 1)
                .put("second", "two")
                .put("third", WON)
                .build();

        assertEquals("{\"first\":1,\"second\":\"two\",\"third\":\"WON\"}", json);
    }

    @Test
    void writesAbsentNumbersAsNull() {
        final var json = JsonWriter.object()
                .putOrNull("missingInt", OptionalInt.empty())
                .putOrNull("missingInteger", Optional.<Integer>empty())
                .putOrNull("presentInt", OptionalInt.of(4))
                .putOrNull("presentInteger", Optional.of(9))
                .build();

        assertEquals("{\"missingInt\":null,\"missingInteger\":null,\"presentInt\":4,\"presentInteger\":9}", json);
    }

    @Test
    void escapesCharactersThatWouldBreakTheDocument() {
        final var json = JsonWriter.object().put("text", "a \"quote\" and a \\ and a \n break").build();

        assertEquals("{\"text\":\"a \\\"quote\\\" and a \\\\ and a \\n break\"}", json);
    }

    @Test
    void escapesControlCharactersAsUnicode() {
        assertEquals("{\"text\":\"\\u0001\"}", JsonWriter.object().put("text", "\u0001").build());
    }

    @Test
    void leavesNonAsciiTextAlone() {
        assertEquals("{\"text\":\"café\"}", JsonWriter.object().put("text", "café").build());
    }
}
