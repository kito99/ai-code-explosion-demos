package tech.virtua.duke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {

    @Test
    @DisplayName("Strings with quotes, slashes and control characters stay valid JSON")
    void quotingEscapesTroubleCharacters() {
        assertEquals("\"he said \\\"hi\\\"\"", Json.quote("he said \"hi\""));
        assertEquals("\"a\\\\b\"", Json.quote("a\\b"));
        assertEquals("\"line\\nbreak\"", Json.quote("line\nbreak"));
        assertEquals("\"bell\\u0007\"", Json.quote("bell" + (char) 7));
    }

    @Test
    @DisplayName("Null members render as JSON null")
    void nullMembersRenderAsNull() {
        assertEquals("{\"a\":null,\"b\":null,\"c\":3}",
                Json.object(Json.member("a", (String) null), Json.member("b", (Integer) null),
                        Json.member("c", 3)));
    }
}
