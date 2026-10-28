package com.example.duke;

import com.example.duke.leaderboard.LeaderboardRepository;
import com.example.duke.support.FixedSecretNumberConfiguration;
import com.example.duke.support.PostgresContainerConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static com.example.duke.game.GameRules.MAX_GUESSES;
import static com.example.duke.support.FixedSecretNumberConfiguration.SECRET_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresContainerConfiguration.class, FixedSecretNumberConfiguration.class})
class GameApiIntegrationTest {

    private static final int WRONG_GUESS = SECRET_NUMBER - 1;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @BeforeEach
    void clearLeaderboard() {
        leaderboardRepository.deleteAll();
    }

    @Test
    @DisplayName("a new game starts with the full guess allowance")
    void startsGame() throws Exception {
        mockMvc.perform(post("/api/games"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.guessesMade").value(0))
                .andExpect(jsonPath("$.guessesRemaining").value(MAX_GUESSES))
                .andExpect(jsonPath("$.secretNumber").doesNotExist())
                .andExpect(jsonPath("$.maxGuesses").value(MAX_GUESSES));
    }

    @Test
    @DisplayName("a wrong guess comes back as too high or too low")
    void answersHighAndLow() throws Exception {
        final var gameId = startGame();

        guess(gameId, SECRET_NUMBER - 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("TOO_LOW"))
                .andExpect(jsonPath("$.guessesMade").value(1))
                .andExpect(jsonPath("$.guessesRemaining").value(MAX_GUESSES - 1));

        guess(gameId, SECRET_NUMBER + 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("TOO_HIGH"))
                .andExpect(jsonPath("$.guessesMade").value(2));
    }

    @Test
    @DisplayName("an out-of-range guess is refused without counting")
    void refusesOutOfRangeGuess() throws Exception {
        final var gameId = startGame();
        guess(gameId, WRONG_GUESS).andExpect(status().isOk());

        guess(gameId, 42)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Please enter a whole number between 1 and 10."));

        guess(gameId, WRONG_GUESS).andExpect(jsonPath("$.guessesMade").value(2));
    }

    @Test
    @DisplayName("a guess that is not a whole number is refused")
    void refusesUnreadableGuess() throws Exception {
        final var gameId = startGame();

        mockMvc.perform(post("/api/games/{gameId}/guesses", gameId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"guess\":\"seven\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Please enter a whole number between 1 and 10."));
    }

    @Test
    @DisplayName("a guess that is not a whole number is refused without counting")
    void refusesFractionalGuess() throws Exception {
        final var gameId = startGame();
        guess(gameId, WRONG_GUESS).andExpect(status().isOk());

        mockMvc.perform(post("/api/games/{gameId}/guesses", gameId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"guess\":3.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Please enter a whole number between 1 and 10."));

        guess(gameId, WRONG_GUESS).andExpect(jsonPath("$.guessesMade").value(2));
    }

    @Test
    @DisplayName("a game id that is not a UUID is answered like a game Duke no longer holds")
    void refusesUnreadableGameId() throws Exception {
        mockMvc.perform(post("/api/games/{gameId}/guesses", "not-a-uuid")
                        .contentType(APPLICATION_JSON)
                        .content("{\"guess\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Start a new game and Duke will think of another number."));
    }

    @Test
    @DisplayName("an unreadable score request does not talk about guesses")
    void refusesUnreadableScoreRequest() throws Exception {
        final var gameId = startGame();
        guess(gameId, SECRET_NUMBER).andExpect(status().isOk());

        mockMvc.perform(post("/api/games/{gameId}/score", gameId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":{\"nested\":1}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Please check what you sent and try again."));
    }

    @Test
    @DisplayName("winning posts a score that shows up on the leaderboard")
    void winsAndPostsScore() throws Exception {
        final var gameId = startGame();
        guess(gameId, WRONG_GUESS).andExpect(status().isOk());

        guess(gameId, SECRET_NUMBER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.outcome").value("CORRECT"))
                .andExpect(jsonPath("$.guessesMade").value(2))
                .andExpect(jsonPath("$.secretNumber").value(SECRET_NUMBER));

        postScore(gameId, "  Duke Fan  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Duke Fan"))
                .andExpect(jsonPath("$.guesses").value(2));

        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].name").value("Duke Fan"))
                .andExpect(jsonPath("$.entries[0].guesses").value(2));
    }

    @Test
    @DisplayName("a win cannot be posted twice")
    void refusesToPostTheSameWinTwice() throws Exception {
        final var gameId = startGame();
        guess(gameId, SECRET_NUMBER).andExpect(status().isOk());
        postScore(gameId, "Duke Fan").andExpect(status().isCreated());

        postScore(gameId, "Duke Fan").andExpect(status().isConflict());

        assertThat(leaderboardRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("a blank name is refused and records nothing")
    void refusesBlankName() throws Exception {
        final var gameId = startGame();
        guess(gameId, SECRET_NUMBER).andExpect(status().isOk());

        postScore(gameId, "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Please enter a name with at least one visible character."));

        assertThat(leaderboardRepository.count()).isZero();
    }

    @Test
    @DisplayName("running out of guesses reveals the number and records nothing")
    void losesAndRecordsNothing() throws Exception {
        final var gameId = startGame();
        for (var attempt = 0; attempt < MAX_GUESSES - 1; attempt++) {
            guess(gameId, WRONG_GUESS).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        guess(gameId, WRONG_GUESS)
                .andExpect(jsonPath("$.status").value("LOST"))
                .andExpect(jsonPath("$.guessesRemaining").value(0))
                .andExpect(jsonPath("$.secretNumber").value(SECRET_NUMBER));

        guess(gameId, SECRET_NUMBER).andExpect(status().isConflict());
        postScore(gameId, "Duke Fan").andExpect(status().isConflict());
        assertThat(leaderboardRepository.count()).isZero();
    }

    @Test
    @DisplayName("an unknown game is reported as missing")
    void reportsUnknownGame() throws Exception {
        guess(UUID.randomUUID(), SECRET_NUMBER).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the leaderboard is empty until a win is posted")
    void servesAnEmptyLeaderboard() throws Exception {
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isEmpty());
    }

    @Test
    @DisplayName("the app serves an OpenAPI definition covering every game action")
    void servesOpenApiDefinition() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/games'].post").exists())
                .andExpect(jsonPath("$.paths['/api/games/{gameId}/guesses'].post").exists())
                .andExpect(jsonPath("$.paths['/api/games/{gameId}/score'].post").exists())
                .andExpect(jsonPath("$.paths['/api/leaderboard'].get").exists())
                .andExpect(jsonPath("$.paths['/api/games/{gameId}/guesses'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/games/{gameId}/score'].post.responses['409']").exists());
    }

    private UUID startGame() throws Exception {
        final var body = mockMvc.perform(post("/api/games"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.gameId"));
    }

    private ResultActions guess(final UUID gameId, final int guess) throws Exception {
        return mockMvc.perform(post("/api/games/{gameId}/guesses", gameId)
                .contentType(APPLICATION_JSON)
                .content("{\"guess\":%d}".formatted(guess)));
    }

    private ResultActions postScore(final UUID gameId, final String name) throws Exception {
        return mockMvc.perform(post("/api/games/{gameId}/score", gameId)
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"%s\"}".formatted(name)));
    }
}
