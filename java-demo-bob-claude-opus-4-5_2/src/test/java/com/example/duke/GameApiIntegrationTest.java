package com.example.duke;

import com.example.duke.game.GameService;
import com.example.duke.game.GameStartResponse;
import com.example.duke.game.GuessRequest;
import com.example.duke.game.GuessResponse;
import com.example.duke.game.GameStatus;
import com.example.duke.leaderboard.LeaderboardEntryDto;
import com.example.duke.leaderboard.PostScoreRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class GameApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GameService gameService;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private GameStartResponse startGame() throws Exception {
        return objectMapper.readValue(
                mockMvc.perform(post("/api/game/start"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                GameStartResponse.class);
    }

    private GuessResponse submitGuess(String gameId, int guess) throws Exception {
        return objectMapper.readValue(
                mockMvc.perform(post("/api/game/guess")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new GuessRequest(gameId, guess))))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                GuessResponse.class);
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void startGame_returns200WithGameId() throws Exception {
        var start = startGame();
        assertThat(start.gameId()).isNotBlank();
        assertThat(start.guessesRemaining()).isEqualTo(5);
    }

    @Test
    void guess_invalidNumberOutOfRange_returns400() throws Exception {
        var start = startGame();

        mockMvc.perform(post("/api/game/guess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GuessRequest(start.gameId(), 11))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void guess_unknownGameId_returns404() throws Exception {
        mockMvc.perform(post("/api/game/guess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GuessRequest("no-such-id", 5))))
                .andExpect(status().isNotFound());
    }

    @Test
    void fullWinFlow_guessCorrectNumberAndPostScore() throws Exception {
        // Keep starting fresh games and guessing sequentially until we win one.
        // With MAX_GUESSES=5 and a range of 1–10, we may need up to two attempts.
        String wonGameId = null;
        outer:
        for (int attempt = 0; attempt < 10; attempt++) {
            var game = gameService.startGame();
            for (int guess = 1; guess <= 10; guess++) {
                var json = mockMvc.perform(post("/api/game/guess")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new GuessRequest(game.gameId(), guess))))
                        .andReturn();
                if (json.getResponse().getStatus() != 200) continue;
                var resp = objectMapper.readValue(json.getResponse().getContentAsString(), GuessResponse.class);
                if (resp.status() == GameStatus.WON) {
                    wonGameId = game.gameId();
                    break outer;
                }
                if (resp.status() == GameStatus.LOST) break; // try fresh game
            }
        }

        assertThat(wonGameId).as("Should have won at least one game in 10 attempts").isNotNull();

        // Post score
        var scoreJson = mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PostScoreRequest(wonGameId, "TestPlayer"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var scoreEntry = objectMapper.readValue(scoreJson, LeaderboardEntryDto.class);
        assertThat(scoreEntry.playerName()).isEqualTo("TestPlayer");
    }

    @Test
    void loseFlow_exhaustGuessesRevealedNumberPopulated() throws Exception {
        var game = gameService.startGame();

        GuessResponse lastResponse = null;
        int wrongGuess = 0;
        for (int attempt = 1; attempt <= 5; attempt++) {
            wrongGuess = (wrongGuess % 10) + 1;
            lastResponse = objectMapper.readValue(
                    mockMvc.perform(post("/api/game/guess")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            new GuessRequest(game.gameId(), wrongGuess))))
                            .andReturn().getResponse().getContentAsString(),
                    GuessResponse.class);

            if (lastResponse.status() == GameStatus.WON) {
                // Hit the secret — restart.
                game = gameService.startGame();
                lastResponse = null;
                wrongGuess = 0;
                attempt = 0;
                continue;
            }
            if (lastResponse.status() == GameStatus.LOST) break;
        }

        assertThat(lastResponse).isNotNull();
        if (lastResponse.status() == GameStatus.LOST) {
            assertThat(lastResponse.revealedNumber()).isNotNull();
            assertThat(lastResponse.feedback()).contains("The number was");
            assertThat(lastResponse.guessesRemaining()).isEqualTo(0);
        }
    }

    @Test
    void guess_afterWinReturns400() throws Exception {
        var game = gameService.startGame();

        for (int guess = 1; guess <= 10; guess++) {
            var mvcResult = mockMvc.perform(post("/api/game/guess")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new GuessRequest(game.gameId(), guess))))
                    .andReturn();
            if (mvcResult.getResponse().getStatus() != 200) break;
            var resp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), GuessResponse.class);
            if (resp.status() == GameStatus.WON) break;
        }

        mockMvc.perform(post("/api/game/guess")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GuessRequest(game.gameId(), 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Game Already Ended"));
    }

    @Test
    void postScore_blankNameReturns400() throws Exception {
        var start = startGame();

        mockMvc.perform(post("/api/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PostScoreRequest(start.gameId(), "   "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leaderboard_returnsArray() throws Exception {
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
