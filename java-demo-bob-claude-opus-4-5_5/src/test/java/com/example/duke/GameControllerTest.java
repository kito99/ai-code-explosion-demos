package com.example.duke.controller;

import com.example.duke.model.GameState;
import com.example.duke.model.GameStatus;
import com.example.duke.service.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    LeaderboardService leaderboardService;

    @BeforeEach
    void clearLeaderboard() {
        leaderboardService.clear();
    }

    @Test
    void indexLoadsWithGameInitialised() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Guess the Number")))
                .andExpect(content().string(containsString("Remaining: 5")));
    }

    @Test
    void guessOutOfRangeDoesNotDecrementGuesses() throws Exception {
        var session = new MockHttpSession();

        // Establish a game
        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());

        // Submit invalid guess (0)
        mockMvc.perform(post("/guess").param("guess", "0").session(session))
                .andExpect(status().is3xxRedirection());

        // Verify counter is unchanged
        mockMvc.perform(get("/").session(session))
                .andExpect(content().string(containsString("Remaining: 5")));
    }

    @Test
    void newGameRestartsFromFresh() throws Exception {
        var session = new MockHttpSession();

        mockMvc.perform(get("/").session(session)).andExpect(status().isOk());

        // Make a valid guess to change state
        mockMvc.perform(post("/guess").param("guess", "1").session(session));

        // Start a new game
        mockMvc.perform(post("/new-game").session(session))
                .andExpect(status().is3xxRedirection());

        // Counter should be back to 5
        mockMvc.perform(get("/").session(session))
                .andExpect(content().string(containsString("Remaining: 5")));
    }

    @Test
    void leaderboardShowsEmptyStateWhenNoEntries() throws Exception {
        mockMvc.perform(get("/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No scores have been posted yet")));
    }

    @Test
    void leaderboardShowsPostedScores() throws Exception {
        leaderboardService.record("Alice", 2);

        mockMvc.perform(get("/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Alice")))
                .andExpect(content().string(containsString("2")));
    }

    @Test
    void postScoreWithBlankNameShowsError() throws Exception {
        var session = new MockHttpSession();

        // Inject a won game into the session
        var wonGame = new GameState(5, 3, 0, GameStatus.WON, "You won!", true);
        session.setAttribute("game", wonGame);

        mockMvc.perform(post("/post-score")
                        .param("playerName", "   ")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("nameError",
                        containsString("enter a name")));
    }
}
