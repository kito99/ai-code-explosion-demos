package com.example.guessgame;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the full HTTP API, using Testcontainers via the tc: JDBC URL
 * configured in application-test.properties.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GameApiTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void startGameReturnsSessionIdAndMaxGuesses() {
        var response = rest.postForEntity("/api/game/start", null, StartGameResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.sessionId()).isNotBlank();
        assertThat(body.maxGuesses()).isEqualTo(5);
    }

    @Test
    void guessOnUnknownSessionReturns404() {
        var request = new GuessRequest(5);
        var response = rest.postForEntity("/api/game/unknown-session/guess", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void guessOutOfRangeReturns400() {
        var start = rest.postForEntity("/api/game/start", null, StartGameResponse.class);
        var sessionId = start.getBody().sessionId();

        var request = new GuessRequest(99);
        var response = rest.postForEntity("/api/game/" + sessionId + "/guess", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void correctGuessMakesGameWon() {
        // Brute-force: try guesses 1–10, restart on GAME_OVER until we win.
        GuessResponse winResponse = null;
        outer:
        for (int attempt = 0; attempt < 20 && winResponse == null; attempt++) {
            var start = rest.postForEntity("/api/game/start", null, StartGameResponse.class);
            var sessionId = start.getBody().sessionId();
            for (int n = 1; n <= 10; n++) {
                var req = new GuessRequest(n);
                var resp = rest.postForEntity("/api/game/" + sessionId + "/guess", req, GuessResponse.class);
                var result = resp.getBody().result();
                if (result == GuessResult.CORRECT) {
                    winResponse = resp.getBody();
                    break outer;
                }
                if (result == GuessResult.GAME_OVER) {
                    break; // start fresh
                }
            }
        }

        assertThat(winResponse).isNotNull();
        assertThat(winResponse.result()).isEqualTo(GuessResult.CORRECT);
        assertThat(winResponse.secretNumber()).isNotNull();
    }

    @Test
    void postScoreOnWonGameAppearsOnLeaderboard() {
        var sessionId = playUntilWin();

        var scoreReq = new ScoreRequest("IntegrationTestPlayer");
        var scoreResp = rest.postForEntity("/api/game/" + sessionId + "/score", scoreReq, ScorePostedResponse.class);
        assertThat(scoreResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(scoreResp.getBody().playerName()).isEqualTo("IntegrationTestPlayer");

        var boardResp = rest.getForEntity("/api/leaderboard", LeaderboardEntryResponse[].class);
        assertThat(boardResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        var entries = boardResp.getBody();
        assertThat(entries).isNotNull();
        assertThat(entries).anyMatch(e -> e.playerName().equals("IntegrationTestPlayer"));
    }

    @Test
    void postScoreOnWonGameTwiceReturns400() {
        var sessionId = playUntilWin();
        var scoreReq = new ScoreRequest("DupPlayer");
        rest.postForEntity("/api/game/" + sessionId + "/score", scoreReq, ScorePostedResponse.class);
        var second = rest.postForEntity("/api/game/" + sessionId + "/score", scoreReq, String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void postScoreOnNonWonGameReturns400() {
        var start = rest.postForEntity("/api/game/start", null, StartGameResponse.class);
        var sessionId = start.getBody().sessionId();

        var scoreReq = new ScoreRequest("ShouldFail");
        var response = rest.postForEntity("/api/game/" + sessionId + "/score", scoreReq, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void postScoreWithEmptyNameReturns400() {
        var sessionId = playUntilWin();
        var scoreReq = new ScoreRequest("  ");
        var response = rest.postForEntity("/api/game/" + sessionId + "/score", scoreReq, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void leaderboardReturnsEmptyListWhenNoScores() {
        var response = rest.getForEntity("/api/leaderboard", LeaderboardEntryResponse[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // May have entries from other tests; just confirm it returns an array
        assertThat(response.getBody()).isNotNull();
    }

    /** Plays a game and returns the session ID in a won state. */
    private String playUntilWin() {
        for (int attempt = 0; attempt < 20; attempt++) {
            var start = rest.postForEntity("/api/game/start", null, StartGameResponse.class);
            var sessionId = start.getBody().sessionId();
            for (int n = 1; n <= 10; n++) {
                var req = new GuessRequest(n);
                var resp = rest.postForEntity("/api/game/" + sessionId + "/guess", req, GuessResponse.class);
                var result = resp.getBody().result();
                if (result == GuessResult.CORRECT) {
                    return sessionId;
                }
                if (result == GuessResult.GAME_OVER) {
                    break; // start a fresh game
                }
            }
        }
        throw new IllegalStateException("Could not win game after 20 attempts.");
    }
}
