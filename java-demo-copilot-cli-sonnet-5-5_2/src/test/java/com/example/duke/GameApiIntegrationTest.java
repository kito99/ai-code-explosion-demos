package com.example.duke;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.duke.leaderboard.ScoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestConfig.class, FixedSecretTestConfig.class})
class GameApiIntegrationTest {

    private static final int SECRET = FixedSecretTestConfig.DEFAULT_SECRET;
    private static final int WRONG_GUESS = 1;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScoreRepository scoreRepository;

    @BeforeEach
    void resetState() {
        scoreRepository.deleteAll();
        FixedSecretTestConfig.NEXT_SECRET.set(SECRET);
    }

    @Test
    void winningAndPostingAScoreShowsUpOnTheLeaderboard() throws Exception {
        final var gameId = startGame();

        guess(gameId, 3).andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("TOO_LOW"))
                .andExpect(jsonPath("$.game.guessesMade").value(1))
                .andExpect(jsonPath("$.game.guessesRemaining").value(4))
                .andExpect(jsonPath("$.game.secretNumber").doesNotExist());
        guess(gameId, 9).andExpect(jsonPath("$.outcome").value("TOO_HIGH"));
        guess(gameId, SECRET).andExpect(jsonPath("$.outcome").value("CORRECT"))
                .andExpect(jsonPath("$.game.status").value("WON"))
                .andExpect(jsonPath("$.game.guessesMade").value(3));
        guess(gameId, SECRET).andExpect(status().isConflict());

        postScore(gameId, "  Duke  ").andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Duke"))
                .andExpect(jsonPath("$.guesses").value(3));

        mockMvc.perform(get("/api/leaderboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].name").value("Duke"))
                .andExpect(jsonPath("$[0].guesses").value(3));
    }

    @Test
    void losingRevealsTheSecretAndRecordsNothing() throws Exception {
        final var gameId = startGame();

        for (var attempt = 1; attempt < 5; attempt++) {
            guess(gameId, WRONG_GUESS).andExpect(jsonPath("$.game.status").value("IN_PROGRESS"));
        }
        guess(gameId, WRONG_GUESS).andExpect(jsonPath("$.game.status").value("LOST"))
                .andExpect(jsonPath("$.game.guessesRemaining").value(0))
                .andExpect(jsonPath("$.game.secretNumber").value(SECRET));

        guess(gameId, SECRET).andExpect(status().isConflict());
        postScore(gameId, "Loser").andExpect(status().isConflict());
        mockMvc.perform(get("/api/leaderboard")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void invalidGuessesGetAFriendlyErrorAndAreNotCounted() throws Exception {
        final var gameId = startGame();

        for (final var body : new String[] {"{\"guess\":0}", "{\"guess\":11}", "{\"guess\":\"abc\"}",
                "{\"guess\":2.5}", "{\"guess\":null}", "{}"}) {
            rawGuess(gameId, body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        guess(gameId, WRONG_GUESS).andExpect(jsonPath("$.game.guessesMade").value(1))
                .andExpect(jsonPath("$.game.guessesRemaining").value(4));
    }

    @Test
    void invalidNamesRecordNothingUntilAValidNameIsPosted() throws Exception {
        final var gameId = startGame();
        guess(gameId, SECRET);

        postScore(gameId, "   ").andExpect(status().isBadRequest());
        postScore(gameId, "x".repeat(31)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("30")));
        mockMvc.perform(get("/api/leaderboard")).andExpect(jsonPath("$", hasSize(0)));

        postScore(gameId, "x".repeat(30)).andExpect(status().isCreated());
        postScore(gameId, "Again").andExpect(status().isConflict());
        mockMvc.perform(get("/api/leaderboard")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void scoresCannotBePostedForGamesInProgressOrUnknownGames() throws Exception {
        final var gameId = startGame();

        postScore(gameId, "Early").andExpect(status().isConflict());
        guess("missing", 3).andExpect(status().isNotFound());
        postScore("missing", "Nobody").andExpect(status().isNotFound());
    }

    @Test
    void leaderboardRanksByFewestGuessesThenEarliestAndShowsTenAtMost() throws Exception {
        win("slow-1", 5);
        win("fast-1", 1);
        win("mid-1", 3);
        win("fast-2", 1);
        win("mid-2", 3);
        for (var index = 1; index <= 8; index++) {
            win("filler-" + index, 4);
        }

        mockMvc.perform(get("/api/leaderboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].name").value("fast-1"))
                .andExpect(jsonPath("$[1].name").value("fast-2"))
                .andExpect(jsonPath("$[2].name").value("mid-1"))
                .andExpect(jsonPath("$[3].name").value("mid-2"))
                .andExpect(jsonPath("$[4].name").value("filler-1"))
                .andExpect(jsonPath("$[9].rank").value(10))
                .andExpect(jsonPath("$[9].name").value("filler-6"))
                .andExpect(content().string(not(containsString("slow-1"))));
    }

    @Test
    void emptyLeaderboardIsAnEmptyList() throws Exception {
        mockMvc.perform(get("/api/leaderboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void servesOpenApiDefinitionCoveringEveryEndpointAndError() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/games'].post").exists())
                .andExpect(jsonPath("$.paths['/api/games/{gameId}/guesses'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/games/{gameId}/guesses'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/games/{gameId}/guesses'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/games/{gameId}/score'].post.requestBody").exists())
                .andExpect(jsonPath("$.paths['/api/leaderboard'].get.responses['200']").exists());
    }

    @Test
    void servesInteractiveDocumentation() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    private void win(final String name, final int guessesNeeded) throws Exception {
        final var gameId = startGame();
        for (var attempt = 1; attempt < guessesNeeded; attempt++) {
            guess(gameId, WRONG_GUESS);
        }
        guess(gameId, SECRET);
        postScore(gameId, name).andExpect(status().isCreated());
    }

    private String startGame() throws Exception {
        final var response = mockMvc.perform(post("/api/games")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.guessesRemaining").value(5))
                .andExpect(jsonPath("$.secretNumber").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("gameId").asText();
    }

    private ResultActions guess(final String gameId, final int value) throws Exception {
        return rawGuess(gameId, String.format("{\"guess\":%d}", value));
    }

    private ResultActions rawGuess(final String gameId, final String body) throws Exception {
        return mockMvc.perform(post("/api/games/{gameId}/guesses", gameId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions postScore(final String gameId, final String name) throws Exception {
        final var body = objectMapper.createObjectNode().put("name", name).toString();
        return mockMvc.perform(post("/api/games/{gameId}/score", gameId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
