package com.example.guessgame.game;

import com.example.guessgame.leaderboard.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.guessgame.leaderboard.LeaderboardController;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for the game controller. Uses @WebMvcTest to avoid needing a database,
 * and mocks the service layer.
 */
@WebMvcTest(controllers = {GameController.class, LeaderboardController.class})
class GameControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    @MockitoBean
    private LeaderboardService leaderboardService;

    private GameSession activeSession() {
        return new GameSession(GameState.ACTIVE, 0, 5, null, null,
                "I'm thinking of a number between 1 and 10. Can you guess it?");
    }

    private GameSession wonSession(int guesses) {
        return new GameSession(GameState.WON, guesses, GameSession.MAX_GUESSES - guesses,
                GuessOutcome.CORRECT, null,
                "🎉 You got it in %d guesses!".formatted(guesses));
    }

    private GameSession lostSession() {
        return new GameSession(GameState.LOST, 5, 0, GuessOutcome.TOO_LOW, 7,
                "No guesses left! The number was 7.");
    }

    @Test
    void homePage_returnsGameTemplate() throws Exception {
        when(gameService.currentGame()).thenReturn(activeSession());
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("game"))
                .andExpect(model().attributeExists("game"));
    }

    @Test
    void newGame_redirectsToHome() throws Exception {
        when(gameService.newGame()).thenReturn(activeSession());
        mockMvc.perform(post("/new-game"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void submitGuess_invalidText_doesNotAdvanceGuessCount() throws Exception {
        var invalidSession = new GameSession(GameState.ACTIVE, 0, 5, GuessOutcome.INVALID, null,
                "Please enter a whole number between 1 and 10.");
        when(gameService.guess(-1)).thenReturn(invalidSession);

        MvcResult result = mockMvc.perform(post("/guess").param("guess", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("game"))
                .andReturn();
        var game = (GameSession) result.getModelAndView().getModel().get("game");
        assertThat(game.guessCount()).isZero();
    }

    @Test
    void submitGuess_outOfRange_doesNotAdvanceGuessCount() throws Exception {
        var invalidSession = new GameSession(GameState.ACTIVE, 0, 5, GuessOutcome.INVALID, null,
                "Please enter a whole number between 1 and 10.");
        when(gameService.guess(0)).thenReturn(invalidSession);

        MvcResult result = mockMvc.perform(post("/guess").param("guess", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("game"))
                .andReturn();
        var game = (GameSession) result.getModelAndView().getModel().get("game");
        assertThat(game.guessCount()).isZero();
    }

    @Test
    void submitGuess_validInRange_advancesGuessCount() throws Exception {
        var afterGuess = new GameSession(GameState.ACTIVE, 1, 4, GuessOutcome.TOO_LOW, null,
                "Too low! Try higher.");
        when(gameService.guess(5)).thenReturn(afterGuess);

        MvcResult result = mockMvc.perform(post("/guess").param("guess", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("game"))
                .andReturn();
        var game = (GameSession) result.getModelAndView().getModel().get("game");
        assertThat(game.guessCount()).isEqualTo(1);
    }

    @Test
    void winState_rendersWinPanel() throws Exception {
        when(gameService.currentGame()).thenReturn(wonSession(3));
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("game"));
    }

    @Test
    void lostState_rendersPlayAgain() throws Exception {
        when(gameService.currentGame()).thenReturn(lostSession());
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("game"));
    }

    @Test
    void postScore_whenNotWon_redirectsToHome() throws Exception {
        when(gameService.currentGame()).thenReturn(activeSession());

        mockMvc.perform(post("/post-score").param("playerName", "Alice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void postScore_invalidName_showsError() throws Exception {
        when(gameService.currentGame()).thenReturn(wonSession(2));

        mockMvc.perform(post("/post-score").param("playerName", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("game"))
                .andExpect(model().attributeExists("nameError"))
                .andExpect(model().attributeExists("game"));
    }

    @Test
    void postScore_validName_redirectsToLeaderboard() throws Exception {
        when(gameService.currentGame()).thenReturn(wonSession(2));
        when(gameService.newGame()).thenReturn(activeSession());

        mockMvc.perform(post("/post-score").param("playerName", "Alice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/leaderboard"));

        verify(leaderboardService).recordWin("Alice", 2);
    }

    @Test
    void leaderboardPage_returns200() throws Exception {
        when(leaderboardService.getTopEntries()).thenReturn(List.of());
        mockMvc.perform(get("/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("leaderboard"))
                .andExpect(model().attributeExists("entries"));
    }
}
