package tech.virtua.duke.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.http.HttpResponse;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checks that the page the browser gets is whole: served, wired to the script, and
/// carrying the artwork the script asks for.
class FrontEndIT {

    private static final Pattern ELEMENT_LOOKUP = Pattern.compile("getElementById\\('([^']+)'\\)");
    private static final Pattern ARTWORK = Pattern.compile("images/[\\w-]+\\.svg");

    private static final GameClient browser = new GameClient();

    private static String page;
    private static String script;

    @BeforeAll
    static void loadTheFrontEnd() {
        page = browser.fetch("/").body();
        script = browser.fetch("/js/app.js").body();
    }

    @Test
    void servesTheGameAtTheRoot() {
        final var response = browser.fetch("/");
        assertAll(
                () -> assertEquals(200, response.statusCode()),
                () -> assertTrue(contentTypeOf(response).startsWith("text/html")),
                () -> assertTrue(page.contains("<title>Guess the Number with Duke</title>")));
    }

    @ParameterizedTest
    @CsvSource({
            "/css/styles.css, text/css",
            "/js/app.js, javascript",
            "/images/duke-thinking.svg, image/svg+xml",
            "/images/duke-wave.svg, image/svg+xml"
    })
    void servesEverythingThePageAsksFor(final String path, final String expectedType) {
        final var response = browser.fetch(path);
        assertAll(
                () -> assertEquals(200, response.statusCode(), path + " should be served"),
                () -> assertTrue(contentTypeOf(response).contains(expectedType),
                        path + " came back as " + contentTypeOf(response)),
                () -> assertFalse(response.body().isBlank(), path + " should not be empty"));
    }

    @Test
    @DisplayName("every element the script reaches for is on the page")
    void thePageAndTheScriptAgreeOnElements() {
        final var wanted = matchesIn(ELEMENT_LOOKUP, script);
        assertFalse(wanted.isEmpty(), "the script should be driving the page");
        assertAll(wanted.stream().map(id ->
                () -> assertTrue(page.contains("id=\"" + id + "\""), "the page is missing id " + id)));
    }

    @Test
    void everyPoseTheScriptSwitchesToIsThere() {
        final var poses = matchesIn(ARTWORK, script);
        assertEquals(2, poses.size(), "Duke thinks in one pose and celebrates in another");
        assertAll(poses.stream().map(pose ->
                () -> assertEquals(200, browser.fetch("/" + pose).statusCode(), pose + " should be served")));
    }

    @Test
    @DisplayName("the markup a keyboard and a screen reader need is on the page")
    void carriesTheMarkupKeyboardAndScreenReaderUseNeeds() {
        assertAll(
                () -> assertTrue(page.contains("<label class=\"guess-label\" for=\"guess\">"),
                        "the guess field is labelled"),
                () -> assertTrue(page.contains("type=\"submit\""), "Enter in the field submits the guess"),
                () -> assertTrue(page.contains("aria-live=\"polite\""), "the result is announced"),
                () -> assertFalse(page.contains("maxlength"),
                        "the field must not trim what was typed before the server sees it"));
    }

    @Test
    void creditsTheArtwork() {
        assertAll(
                () -> assertTrue(page.contains("github.com/openjdk/duke")),
                () -> assertTrue(browser.fetch("/images/duke-wave.svg").body().contains("OpenJDK Duke project")));
    }

    private static Set<String> matchesIn(final Pattern pattern, final String text) {
        return pattern.matcher(text).results()
                .map(match -> match.groupCount() == 0 ? match.group() : match.group(1))
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private static String contentTypeOf(final HttpResponse<String> response) {
        return response.headers().firstValue("Content-Type").orElse("");
    }
}
