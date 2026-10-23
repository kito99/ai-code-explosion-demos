package tech.virtua.duke;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/// Builds small JSON objects, so the game needs no JSON dependency. Field order
/// follows insertion order.
public final class JsonWriter {

    private static final String NULL_LITERAL = "null";

    private final Map<String, String> fields = new LinkedHashMap<>();

    private JsonWriter() {
    }

    /// An empty JSON object ready to accept fields.
    public static JsonWriter object() {
        return new JsonWriter();
    }

    /// Adds a string field.
    public JsonWriter put(final String name, final String value) {
        return putRaw(name, quote(value));
    }

    /// Adds a numeric field.
    public JsonWriter put(final String name, final int value) {
        return putRaw(name, Integer.toString(value));
    }

    /// Adds an enum field, written as its constant name.
    public JsonWriter put(final String name, final Enum<?> value) {
        return putRaw(name, quote(requireNonNull(value).name()));
    }

    /// Adds a numeric field, or `null` when the value is absent.
    public JsonWriter putOrNull(final String name, final OptionalInt value) {
        return putRaw(name, requireNonNull(value).isPresent()
                ? Integer.toString(value.getAsInt())
                : NULL_LITERAL);
    }

    /// Adds a numeric field, or `null` when the value is absent.
    public JsonWriter putOrNull(final String name, final Optional<Integer> value) {
        return putRaw(name, requireNonNull(value).map(String::valueOf).orElse(NULL_LITERAL));
    }

    /// The rendered JSON object.
    public String build() {
        return fields.entrySet().stream()
                .map(field -> format("%s:%s", quote(field.getKey()), field.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private JsonWriter putRaw(final String name, final String renderedValue) {
        fields.put(requireNonNull(name), renderedValue);
        return this;
    }

    private static String quote(final String value) {
        final var escaped = new StringBuilder(requireNonNull(value).length() + 2).append('"');
        value.chars().forEach(character -> escaped.append(escapeCharacter(character)));
        return escaped.append('"').toString();
    }

    private static String escapeCharacter(final int character) {
        return switch (character) {
            case '"' -> "\\\"";
            case '\\' -> "\\\\";
            case '\b' -> "\\b";
            case '\f' -> "\\f";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> character < ' ' ? format("\\u%04x", character) : Character.toString(character);
        };
    }
}
