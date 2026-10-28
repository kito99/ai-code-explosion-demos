package com.example.duke;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Import(DukeIntegrationTest.Config.class)
class DukeIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Config {
        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>(DockerImageName.parse("postgres:18.0"));
        }

        // Always picks 7 so outcomes are deterministic
        @Bean
        @Primary
        java.util.random.RandomGenerator fixedRandom() {
            return new java.util.Random() {
                @Override public int nextInt(int origin, int bound) { return 7; }
            };
        }
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM leaderboard_entry");
    }

    private void guess(MockHttpSession s, String json, String status) throws Exception {
        mvc.perform(post("/api/game/guess").session(s).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(jsonPath("$.status", is(status)));
    }

    @Test
    void invalidGuessDoesNotCountAndFeedbackIsGiven() throws Exception {
        var s = new MockHttpSession();
        mvc.perform(post("/api/game").session(s)).andExpect(jsonPath("$.guessesRemaining", is(5)));
        for (String bad : new String[] {"\"0\"", "\"11\"", "\"abc\"", "\"3.5\"", "\"\""}) {
            mvc.perform(post("/api/game/guess").session(s).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"guess\":" + bad + "}"))
                    .andExpect(jsonPath("$.status", is("INVALID")))
                    .andExpect(jsonPath("$.guessesRemaining", is(5)));
        }
        guess(s, "{\"guess\":\"3\"}", "TOO_LOW");
        guess(s, "{\"guess\":\"9\"}", "TOO_HIGH");
    }

    @Test
    void loseRevealsSecretAndRejectsFurtherGuessesAndScores() throws Exception {
        var s = new MockHttpSession();
        mvc.perform(post("/api/game").session(s));
        for (int i = 0; i < 4; i++) guess(s, "{\"guess\":\"1\"}", "TOO_LOW");
        mvc.perform(post("/api/game/guess").session(s).contentType(MediaType.APPLICATION_JSON).content("{\"guess\":\"1\"}"))
                .andExpect(jsonPath("$.status", is("LOST")))
                .andExpect(jsonPath("$.secret", is(7)));
        mvc.perform(post("/api/game/guess").session(s).contentType(MediaType.APPLICATION_JSON).content("{\"guess\":\"7\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/game/score").session(s).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bob\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/leaderboard")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void winPostsScoreOnceWithNameValidation() throws Exception {
        var s = new MockHttpSession();
        mvc.perform(post("/api/game").session(s));
        guess(s, "{\"guess\":\"2\"}", "TOO_LOW");
        guess(s, "{\"guess\":\"7\"}", "WON");
        mvc.perform(post("/api/game/score").session(s).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
                .andExpect(jsonPath("$.status", is("INVALID_NAME")));
        mvc.perform(post("/api/game/score").session(s).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  Ada  \"}"))
                .andExpect(jsonPath("$.status", is("POSTED")));
        mvc.perform(post("/api/game/score").session(s).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ada\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/leaderboard"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rank", is(1)))
                .andExpect(jsonPath("$[0].name", is("Ada")))
                .andExpect(jsonPath("$[0].guesses", is(2)));
    }

    @Test
    void leaderboardOrdersByGuessesThenRecordedAndCapsAtTen() throws Exception {
        for (int i = 0; i < 12; i++) {
            jdbc.update("INSERT INTO leaderboard_entry (name, guesses) VALUES (?, ?)", "p" + i, i == 11 ? 1 : 3);
        }
        mvc.perform(get("/api/leaderboard"))
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].name", is("p11")))
                .andExpect(jsonPath("$[1].name", is("p0")))
                .andExpect(jsonPath("$[9].name", is("p8")));
    }
}
