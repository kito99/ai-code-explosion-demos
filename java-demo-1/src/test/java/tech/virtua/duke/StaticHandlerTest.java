package tech.virtua.duke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StaticHandlerTest {

    @Test
    @DisplayName("The root path serves the game page")
    void rootMapsToIndex() {
        assertEquals("web/index.html", StaticHandler.resourceFor("/"));
        assertEquals("web/index.html", StaticHandler.resourceFor(""));
        assertEquals("web/styles.css", StaticHandler.resourceFor("/styles.css"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/../pom.xml", "/web/../../secret", "//evil", "relative.css", "/a/../../b"})
    @DisplayName("Paths that try to escape the web folder are refused")
    void traversalIsRefused(String path) {
        assertNull(StaticHandler.resourceFor(path));
    }
}
