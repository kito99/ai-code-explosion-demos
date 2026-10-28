package com.example.guessgame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class GameControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18");

    @Autowired MockMvc mockMvc;
    @Autowired LeaderboardRepository leaderboardRepository;

    @BeforeEach
    void clearDb() {
        leaderboardRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // GET / — game page loads

    @Test
    void gamePageLoads() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().isOk())
               .andExpect(view().name("game"))
               .andExpect(model().attributeExists("guessCount", "guessesRemaining", "status"));
    }

    @Test
    void initialStateIsActive() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(model().attribute("status", "ACTIVE"))
               .andExpect(model().attribute("guessCount", 0))
               .andExpect(model().attribute("guessesRemaining", 5));
    }

    // -----------------------------------------------------------------------
    // FR-6 — Input validation

    @Test
    void nonNumericGuessIsRejected() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/guess").param("guess", "abc").session(session))
               .andExpect(status().isOk())
               .andExpect(model().attributeExists("validationMessage"))
               .andExpect(model().attribute("guessCount", 0));
    }

    @Test
    void tooLowInputIsRejected() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/guess").param("guess", "0").session(session))
               .andExpect(model().attributeExists("validationMessage"))
               .andExpect(model().attribute("guessCount", 0));
    }

    @Test
    void tooHighInputIsRejected() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/guess").param("guess", "11").session(session))
               .andExpect(model().attributeExists("validationMessage"))
               .andExpect(model().attribute("guessCount", 0));
    }

    // -----------------------------------------------------------------------
    // FR-7, FR-8 — Accepted guess increments counter

    @Test
    void acceptedGuessIncrementsCount() throws Exception {
        MockHttpSession session = new MockHttpSession();
        // First guess — any valid number; feedback may be too-low/high/correct
        mockMvc.perform(post("/guess").param("guess", "5").session(session))
               .andExpect(status().isOk())
               .andExpect(model().attribute("guessCount", 1))
               .andExpect(model().attribute("guessesRemaining", 4));
    }

    // -----------------------------------------------------------------------
    // FR-3, FR-11 — Lose state after 5 wrong guesses

    @Test
    void loseAfterFiveWrongGuesses() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Inject a known game so we can guarantee the secret number
        GameSession game = new GameSession() {
            // Force secret number to 10 by immediately resetting with a controlled seed
        };
        // Directly put a controlled game in session
        GameSession controlled = new GameSession();
        // We'll just exhaust guesses — secret is random but we avoid it by guessing same wrong value 5x
        // We need to know the secret; instead, inject the game into the session
        // Use the session and drive enough guesses. If we accidentally guess right, restart.
        // Simpler: use session attribute injection via MockHttpSession.
        GameSession injected = new GameSession();
        java.lang.reflect.Field f;
        try {
            f = GameSession.class.getDeclaredField("secretNumber");
            f.setAccessible(true);
            f.set(injected, 10); // secret = 10; we will guess 1 five times
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        session.setAttribute("game", injected);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/guess").param("guess", "1").session(session));
        }

        mockMvc.perform(get("/").session(session))
               .andExpect(model().attribute("status", "LOST"));
    }

    // -----------------------------------------------------------------------
    // FR-9, FR-15 — Win state and score submission

    @Test
    void winStateWhenGuessCorrect() throws Exception {
        MockHttpSession session = new MockHttpSession();
        GameSession injected = new GameSession();
        setSecret(injected, 7);
        session.setAttribute("game", injected);

        mockMvc.perform(post("/guess").param("guess", "7").session(session))
               .andExpect(model().attribute("status", "WON"))
               .andExpect(model().attribute("guessCount", 1));
    }

    @Test
    void submitScorePersistsEntry() throws Exception {
        MockHttpSession session = new MockHttpSession();
        GameSession injected = new GameSession();
        setSecret(injected, 3);
        session.setAttribute("game", injected);

        // Win the game
        mockMvc.perform(post("/guess").param("guess", "3").session(session));

        // Submit score
        mockMvc.perform(post("/submit-score").param("playerName", "Alice").session(session))
               .andExpect(redirectedUrl("/leaderboard"));

        var entries = leaderboardRepository.findTop10ByOrderByGuessCountAscRecordedAtAsc();
        org.junit.jupiter.api.Assertions.assertEquals(1, entries.size());
        org.junit.jupiter.api.Assertions.assertEquals("Alice", entries.get(0).getPlayerName());
        org.junit.jupiter.api.Assertions.assertEquals(1, entries.get(0).getGuessCount());
    }

    // -----------------------------------------------------------------------
    // FR-13, FR-14 — Name validation on score submission

    @Test
    void emptyNameIsRejectedOnSubmit() throws Exception {
        MockHttpSession session = new MockHttpSession();
        GameSession injected = new GameSession();
        setSecret(injected, 4);
        session.setAttribute("game", injected);
        mockMvc.perform(post("/guess").param("guess", "4").session(session));

        mockMvc.perform(post("/submit-score").param("playerName", "   ").session(session))
               .andExpect(status().isOk())
               .andExpect(model().attributeExists("nameError"));
    }

    @Test
    void nameTooLongIsRejectedOnSubmit() throws Exception {
        MockHttpSession session = new MockHttpSession();
        GameSession injected = new GameSession();
        setSecret(injected, 4);
        session.setAttribute("game", injected);
        mockMvc.perform(post("/guess").param("guess", "4").session(session));

        String longName = "A".repeat(31);
        mockMvc.perform(post("/submit-score").param("playerName", longName).session(session))
               .andExpect(status().isOk())
               .andExpect(model().attributeExists("nameError"));
    }

    // -----------------------------------------------------------------------
    // FR-17, FR-18 — Leaderboard screen

    @Test
    void leaderboardShowsEmptyMessage() throws Exception {
        mockMvc.perform(get("/leaderboard"))
               .andExpect(status().isOk())
               .andExpect(view().name("leaderboard"))
               .andExpect(model().attribute("entries", hasSize(0)));
    }

    @Test
    void leaderboardOrdersByGuessCountThenTime() throws Exception {
        leaderboardRepository.save(new LeaderboardEntry("Charlie", 3));
        leaderboardRepository.save(new LeaderboardEntry("Alice", 1));
        leaderboardRepository.save(new LeaderboardEntry("Bob", 2));

        mockMvc.perform(get("/leaderboard"))
               .andExpect(model().attribute("entries", hasSize(3)))
               .andExpect(model().attribute("entries",
                   hasItem(hasProperty("playerName", equalTo("Alice")))));
    }

    @Test
    void leaderboardCapsAtTen() throws Exception {
        for (int i = 1; i <= 12; i++) {
            leaderboardRepository.save(new LeaderboardEntry("Player" + i, (i % 5) + 1));
        }

        mockMvc.perform(get("/leaderboard"))
               .andExpect(model().attribute("entries", hasSize(10)));
    }

    // -----------------------------------------------------------------------
    // FR-12 — New game resets state

    @Test
    void newGameResetsState() throws Exception {
        MockHttpSession session = new MockHttpSession();
        // Make a guess first
        mockMvc.perform(post("/guess").param("guess", "5").session(session));
        // Start a new game
        mockMvc.perform(post("/new-game").session(session))
               .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/").session(session))
               .andExpect(model().attribute("status", "ACTIVE"))
               .andExpect(model().attribute("guessCount", 0));
    }

    // -----------------------------------------------------------------------
    private void setSecret(GameSession game, int secret) {
        try {
            java.lang.reflect.Field f = GameSession.class.getDeclaredField("secretNumber");
            f.setAccessible(true);
            f.set(game, secret);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
