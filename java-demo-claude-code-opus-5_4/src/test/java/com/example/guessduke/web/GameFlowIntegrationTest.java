package com.example.guessduke.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import com.example.guessduke.PostgresTestcontainerConfiguration;
import com.example.guessduke.game.FixedSecretNumberGenerator;

import static com.example.guessduke.game.GameRules.MAX_GUESSES;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestcontainerConfiguration.class, GameFlowIntegrationTest.FixedSecretConfiguration.class})
class GameFlowIntegrationTest {

    private static final int SECRET = 7;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private FixedSecretNumberGenerator secretNumberGenerator;

    private MockHttpSession session;

    @BeforeEach
    void startFromAnEmptyLeaderboard() {
        jdbcClient.sql("TRUNCATE TABLE leaderboard_entry RESTART IDENTITY").update();
        secretNumberGenerator.willChoose(SECRET);
        session = new MockHttpSession();
    }

    @Test
    void aNewGameStartsWithEveryGuessAvailableAndNoSecretRevealed() throws Exception {
        startGame()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.guessesMade").value(0))
                .andExpect(jsonPath("$.guessesRemaining").value(MAX_GUESSES))
                .andExpect(jsonPath("$.secretNumber").doesNotExist())
                .andExpect(jsonPath("$.scorePosted").value(false));
    }

    @Test
    void unusableInputCostsNoGuess() throws Exception {
        startGame();

        guess("eleven")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.guessesMade").value(0))
                .andExpect(jsonPath("$.guessesRemaining").value(MAX_GUESSES))
                .andExpect(jsonPath("$.message").value(GameMessages.GUESS_NOT_ACCEPTED));
    }

    @Test
    void playingAWinningRoundPostsOneRankedLeaderboardEntry() throws Exception {
        startGame();
        guess("2").andExpect(jsonPath("$.message").value(containsString("too low")));
        guess("9").andExpect(jsonPath("$.message").value(containsString("too high")));
        guess(String.valueOf(SECRET))
                .andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.guessesMade").value(3));

        postScore("  Duke  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posted").value(true));

        readLeaderboard()
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].playerName").value("Duke"))
                .andExpect(jsonPath("$[0].guessCount").value(3));
    }

    @Test
    void postingTheSameWinTwiceRecordsOneEntry() throws Exception {
        winImmediately();
        postScore("Duke").andExpect(jsonPath("$.posted").value(true));

        postScore("Duke")
                .andExpect(jsonPath("$.posted").value(false))
                .andExpect(jsonPath("$.message").value(GameMessages.SCORE_ALREADY_POSTED));

        readLeaderboard().andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void anUnusableNameRecordsNothing() throws Exception {
        winImmediately();

        postScore("   ")
                .andExpect(jsonPath("$.posted").value(false))
                .andExpect(jsonPath("$.message").value(GameMessages.NAME_NOT_ACCEPTED));

        readLeaderboard().andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void decliningToPostRecordsNothing() throws Exception {
        winImmediately();

        startGame().andExpect(jsonPath("$.status").value("ACTIVE"));

        readLeaderboard().andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void aLostRoundRevealsTheSecretAndRecordsNothing() throws Exception {
        startGame();
        for (var attempt = 1; attempt < MAX_GUESSES; attempt++) {
            guess("1").andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        guess("1")
                .andExpect(jsonPath("$.status").value("LOST"))
                .andExpect(jsonPath("$.guessesRemaining").value(0))
                .andExpect(jsonPath("$.secretNumber").value(SECRET));

        postScore("Duke").andExpect(jsonPath("$.posted").value(false));
        readLeaderboard().andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void aFinishedRoundAcceptsNoFurtherGuesses() throws Exception {
        winImmediately();

        guess("1")
                .andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.guessesMade").value(1))
                .andExpect(jsonPath("$.message").value(GameMessages.ROUND_ALREADY_OVER));
    }

    @Test
    void aGuessWithoutASessionStartsAFreshRoundInstead() throws Exception {
        guess(String.valueOf(SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.guessesMade").value(0))
                .andExpect(jsonPath("$.message").value(GameMessages.SESSION_RESTARTED));
    }

    @Test
    void theLeaderboardShowsEveryPlayersResultsRankedTogether() throws Exception {
        winIn(3, "Steady");
        winIn(1, "Lucky");
        winIn(2, "Sharp");

        readLeaderboard()
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].playerName").value("Lucky"))
                .andExpect(jsonPath("$[1].playerName").value("Sharp"))
                .andExpect(jsonPath("$[2].playerName").value("Steady"));
    }

    @Test
    void anEmptyLeaderboardReturnsNoRows() throws Exception {
        readLeaderboard().andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    private void winImmediately() throws Exception {
        startGame();
        guess(String.valueOf(SECRET)).andExpect(jsonPath("$.status").value("WON"));
    }

    /// Plays a separate player's round, as though it arrived from another browser.
    private void winIn(final int guessesTaken, final String playerName) throws Exception {
        session = new MockHttpSession();
        startGame();
        for (var attempt = 1; attempt < guessesTaken; attempt++) {
            guess(String.valueOf(SECRET + 1));
        }
        guess(String.valueOf(SECRET));
        postScore(playerName).andExpect(jsonPath("$.posted").value(true));
    }

    private ResultActions startGame() throws Exception {
        return mockMvc.perform(post("/api/game").session(session));
    }

    private ResultActions guess(final String rawGuess) throws Exception {
        return mockMvc.perform(post("/api/game/guesses")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content("{\"guess\":%s}".formatted(quoted(rawGuess))));
    }

    private ResultActions postScore(final String rawName) throws Exception {
        return mockMvc.perform(post("/api/game/scores")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content("{\"name\":%s}".formatted(quoted(rawName))));
    }

    private ResultActions readLeaderboard() throws Exception {
        return mockMvc.perform(get("/api/leaderboard"));
    }

    private static String quoted(final String value) {
        return "\"%s\"".formatted(value);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedSecretConfiguration {

        @Bean
        @Primary
        FixedSecretNumberGenerator fixedSecretNumberGenerator() {
            return new FixedSecretNumberGenerator();
        }
    }
}
