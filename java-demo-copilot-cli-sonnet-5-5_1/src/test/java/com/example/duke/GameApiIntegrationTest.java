package com.example.duke;

import com.example.duke.leaderboard.LeaderboardRepository;
import com.example.duke.support.FixedSecretConfiguration;
import com.example.duke.support.PostgresContainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import static com.example.duke.support.FixedSecretConfiguration.SECRET;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresContainerConfiguration.class, FixedSecretConfiguration.class})
class GameApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LeaderboardRepository repository;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void clearLeaderboard() {
        repository.deleteAll();
    }

    private String startGame() throws Exception {
        final var body = mvc.perform(post("/api/games")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.guessesRemaining").value(5))
                .andExpect(jsonPath("$.secretNumber").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("gameId").asString();
    }

    private ResultActions guess(final String gameId, final String guessJson) throws Exception {
        return mvc.perform(post("/api/games/{id}/guesses", gameId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"guess\":" + guessJson + "}"));
    }

    private ResultActions postScore(final String gameId, final String name) throws Exception {
        return mvc.perform(post("/api/games/{id}/score", gameId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"));
    }

    @Test
    void winningAndPostingAScoreShowsOnTheLeaderboard() throws Exception {
        final var gameId = startGame();
        guess(gameId, "3").andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("TOO_LOW"))
                .andExpect(jsonPath("$.guessesRemaining").value(4));
        guess(gameId, "9").andExpect(jsonPath("$.outcome").value("TOO_HIGH"));
        guess(gameId, String.valueOf(SECRET)).andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.guessesMade").value(3));

        postScore(gameId, "  Duke  ").andExpect(status().isCreated());
        postScore(gameId, "Duke").andExpect(status().isConflict());

        mvc.perform(get("/api/leaderboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].name").value("Duke"))
                .andExpect(jsonPath("$.entries[0].guesses").value(3));
    }

    @Test
    void losingRevealsTheSecretAndRecordsNothing() throws Exception {
        final var gameId = startGame();
        for (int attempt = 0; attempt < 5; attempt++) {
            guess(gameId, "1").andExpect(status().isOk());
        }
        guess(gameId, "1").andExpect(status().isConflict());
        guess(gameId, "1");
        postScore(gameId, "Loser").andExpect(status().isConflict());

        mvc.perform(get("/api/leaderboard")).andExpect(jsonPath("$.entries.length()").value(0));
    }

    @Test
    void lostGameResponseCarriesTheSecret() throws Exception {
        final var gameId = startGame();
        for (int attempt = 0; attempt < 4; attempt++) {
            guess(gameId, "1");
        }
        guess(gameId, "1").andExpect(jsonPath("$.status").value("LOST"))
                .andExpect(jsonPath("$.secretNumber").value(SECRET));
    }

    @Test
    void invalidGuessesAreRejectedWithoutCounting() throws Exception {
        final var gameId = startGame();
        for (final var bad : new String[]{"0", "11", "2.5", "\"abc\"", "null"}) {
            guess(gameId, bad).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        }
        guess(gameId, "1").andExpect(jsonPath("$.guessesMade").value(1));
    }

    @Test
    void invalidNamesAreRejected() throws Exception {
        final var gameId = startGame();
        guess(gameId, String.valueOf(SECRET));
        postScore(gameId, "   ").andExpect(status().isBadRequest());
        postScore(gameId, "x".repeat(31)).andExpect(status().isBadRequest());
        postScore(gameId, "x".repeat(30)).andExpect(status().isCreated());
    }

    @Test
    void unknownGameIsNotFound() throws Exception {
        guess("00000000-0000-0000-0000-000000000000", "3").andExpect(status().isNotFound());
        guess("not-a-uuid", "3").andExpect(status().isNotFound());
    }

    @Test
    void servesOpenApiDefinition() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/games']").exists())
                .andExpect(jsonPath("$.paths['/api/games/{gameId}/guesses']").exists())
                .andExpect(jsonPath("$.paths['/api/games/{gameId}/score']").exists())
                .andExpect(jsonPath("$.paths['/api/leaderboard']").exists());
    }
}
