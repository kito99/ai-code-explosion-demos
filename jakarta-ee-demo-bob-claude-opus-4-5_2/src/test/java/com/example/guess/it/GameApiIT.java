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
        RestAssured.port    = LibertyServerExtension.LIBERTY_PORT;
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
            .statusCode(422)
            .body("message", notNullValue());
    }

    @Test
    @Order(3)
    void invalidHighGuessReturns422() {
        var sessionId = startNewSession();

        given()
            .contentType(ContentType.JSON)
            .body("{\"guess\": 11}")
        .when()
            .post("/game/{id}/guess", sessionId)
        .then()
            .statusCode(422)
            .body("message", notNullValue());
    }

    @Test
    @Order(4)
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
    @Order(5)
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
    @Order(6)
    void leaderboardReturnsArray() {
        given()
            .when().get("/leaderboard")
            .then()
            .statusCode(200)
            .body("$", instanceOf(java.util.List.class));
    }

    @Test
    @Order(7)
    void unknownSessionReturns404() {
        given()
            .when().get("/game/unknown-session-id")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(8)
    void postScoreWithBlankNameReturns422() {
        var sessionId = winGame();

        given()
            .contentType(ContentType.JSON)
            .body("{\"playerName\": \"   \"}")
        .when()
            .post("/leaderboard/{id}", sessionId)
        .then()
            .statusCode(422)
            .body("message", notNullValue());
    }

    @Test
    @Order(9)
    void postScoreWithTooLongNameReturns422() {
        var sessionId = winGame();
        var longName = "A".repeat(31);

        given()
            .contentType(ContentType.JSON)
            .body("{\"playerName\": \"" + longName + "\"}")
        .when()
            .post("/leaderboard/{id}", sessionId)
        .then()
            .statusCode(422)
            .body("message", notNullValue());
    }

    @Test
    @Order(10)
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
     * Starts a game and guesses sequentially 1–10 until CORRECT.
     * Starts a fresh game if the guess limit is exhausted before finding the number.
     */
    private static String winGame() {
        for (int attempt = 0; attempt < 10; attempt++) {
            var sessionId = startNewSession();
            for (int guess = 1; guess <= 10; guess++) {
                var feedback = given()
                        .contentType(ContentType.JSON)
                        .body("{\"guess\": " + guess + "}")
                        .when()
                        .post("/game/{id}/guess", sessionId)
                        .then().extract().path("feedback").toString();
                if ("CORRECT".equals(feedback)) {
                    return sessionId;
                }
                if ("GAME_OVER".equals(feedback)) {
                    break; // start a fresh session
                }
            }
        }
        throw new IllegalStateException("Could not win a game in 10 attempts");
    }
}
