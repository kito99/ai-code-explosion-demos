package com.example.guess.it;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the game and leaderboard REST API.
 *
 * {@link LibertyServerExtension} starts a PostgreSQL Testcontainer and the
 * Liberty server before the suite runs, and shuts both down afterwards.
 * No external database needs to be running.
 */
@ExtendWith(LibertyServerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GameApiIT {

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = LibertyServerExtension.LIBERTY_PORT;
        RestAssured.basePath = "/api";
    }

    @Test
    @Order(1)
    void startGameReturns201WithSessionId() {
        given()
            .when().post("/game/start")
            .then()
            .statusCode(201)
            .body("sessionId", notNullValue())
            .body("active", is(true))
            .body("guessCount", is(0))
            .body("guessesRemaining", is(5));
    }

    @Test
    @Order(2)
    void invalidGuessReturns422() {
        var sessionId = startNewSession();

        given()
            .contentType(ContentType.JSON)
            .body("{\"guess\": 0}")
        .when()
            .post("/game/{id}/guess", sessionId)
        .then()
            .statusCode(422);
    }

    @Test
    @Order(3)
    void validGuessReturnsFeedback() {
        var sessionId = startNewSession();

        given()
            .contentType(ContentType.JSON)
            .body("{\"guess\": 5}")
        .when()
            .post("/game/{id}/guess", sessionId)
        .then()
            .statusCode(200)
            .body("feedback", oneOf("TOO_LOW", "TOO_HIGH", "CORRECT"))
            .body("guessCount", is(1));
    }

    @Test
    @Order(4)
    void leaderboardPostRequiresWonSession() {
        var sessionId = startNewSession();

        given()
            .contentType(ContentType.JSON)
            .body("{\"playerName\": \"Alice\"}")
        .when()
            .post("/leaderboard/{id}", sessionId)
        .then()
            .statusCode(409);
    }

    @Test
    @Order(5)
    void leaderboardReturnsArray() {
        given()
            .when().get("/leaderboard")
            .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    @Order(6)
    void unknownSessionReturns404() {
        given()
            .when().get("/game/unknown-session-id")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(7)
    void postScoreWithEmptyNameReturns400() {
        var sessionId = winGame();

        given()
            .contentType(ContentType.JSON)
            .body("{\"playerName\": \"   \"}")
        .when()
            .post("/leaderboard/{id}", sessionId)
        .then()
            .statusCode(400);
    }

    @Test
    @Order(8)
    void postScoreWithTooLongNameReturns400() {
        var sessionId = winGame();
        var longName = "A".repeat(31);

        given()
            .contentType(ContentType.JSON)
            .body("{\"playerName\": \"" + longName + "\"}")
        .when()
            .post("/leaderboard/{id}", sessionId)
        .then()
            .statusCode(400);
    }

    @Test
    @Order(9)
    void winAndPostScoreAppearsOnLeaderboard() {
        var sessionId = winGame();

        given()
            .contentType(ContentType.JSON)
            .body("{\"playerName\": \"TestPlayer\"}")
        .when()
            .post("/leaderboard/{id}", sessionId)
        .then()
            .statusCode(201);

        given()
            .when().get("/leaderboard")
            .then()
            .statusCode(200)
            .body("playerName", hasItem("TestPlayer"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String startNewSession() {
        return given()
                .when().post("/game/start")
                .then().statusCode(201)
                .extract().path("sessionId");
    }

    /**
     * Starts a game and keeps guessing 1–10 until CORRECT, then returns the session id.
     */
    private static String winGame() {
        var sessionId = startNewSession();
        for (int i = 1; i <= 10; i++) {
            var feedback = given()
                    .contentType(ContentType.JSON)
                    .body("{\"guess\": " + i + "}")
                    .when()
                    .post("/game/{id}/guess", sessionId)
                    .then().extract().path("feedback").toString();
            if ("CORRECT".equals(feedback)) {
                return sessionId;
            }
        }
        throw new IllegalStateException("Could not win game in 10 guesses");
    }
}
