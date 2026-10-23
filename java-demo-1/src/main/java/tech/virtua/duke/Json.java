package tech.virtua.duke;

import java.util.StringJoiner;

/** Just enough JSON to serialize a {@link GameState} without pulling in a library. */
final class Json {

    private Json() {
    }

    static String object(String... members) {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        for (String member : members) {
            joiner.add(member);
        }
        return joiner.toString();
    }

    static String member(String name, String value) {
        return quote(name) + ":" + (value == null ? "null" : quote(value));
    }

    static String member(String name, int value) {
        return quote(name) + ":" + value;
    }

    static String member(String name, Integer value) {
        return quote(name) + ":" + (value == null ? "null" : value.toString());
    }

    static String member(String name, boolean value) {
        return quote(name) + ":" + value;
    }

    static String quote(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 2).append('"');
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
