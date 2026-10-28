package com.example.guess.it;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the REST API.
 * {@link LibertyServerExtension} starts a PostgreSQL Testcontainers container and the
 * Liberty server before these tests run, then stops both afterward.
 */
@ExtendWith(LibertyServerExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GameApiIT {

    @BeforeAll
    static void setUpRestAssured() {
        RestAssured.baseURI  = "http://localhost";
        RestAssured.port     = 9080;
        RestAssured.basePath = "/jakarta-ee-demo-bob-claude-opus-4-5_3/api";
    }

    // ---- /game ----

    @Test
    @Order(1)
    void startGameReturnsActiveState() {
        given().contentType(ContentType.JSON)
               .when().post("/game/start")
               .then()
               .statusCode(200)
               .body("active",           equalTo(true))
               .body("won",              equalTo(false))
               .body("guessesMade",      equalTo(0))
               .body("guessesRemaining", equalTo(5));
    }

    @Test
    @Order(2)
    void getStateReturnsCurrentGame() {
        given().contentType(ContentType.JSON).post("/game/start").then().statusCode(200);

        given().accept(ContentType.JSON)
               .when().get("/game/state")
               .then()
               .statusCode(200)
               .body("active", notNullValue());
    }

    @Test
    @Order(3)
    void invalidGuessOutOfRangeReturns400() {
        given().contentType(ContentType.JSON).post("/game/start").then().statusCode(200);

        given().contentType(ContentType.JSON)
               .body("{\"guess\": 11}")
               .when().post("/game/guess")
               .then()
               .statusCode(400)
               .body("message", notNullValue());
    }

    @Test
    @Order(4)
    void invalidGuessMissingFieldReturns400() {
        given().contentType(ContentType.JSON).post("/game/start").then().statusCode(200);

        given().contentType(ContentType.JSON)
               .body("{}")
               .when().post("/game/guess")
               .then()
               .statusCode(400);
    }

    @Test
    @Order(5)
    void validGuessReturnsGuessResult() {
        given().contentType(ContentType.JSON).post("/game/start").then().statusCode(200);

        given().contentType(ContentType.JSON)
               .body("{\"guess\": 5}")
               .when().post("/game/guess")
               .then()
               .statusCode(200)
               .body("feedback",         notNullValue())
               .body("guessesMade",      equalTo(1))
               .body("guessesRemaining", lessThanOrEqualTo(4));
    }

    // ---- /leaderboard ----

    @Test
    @Order(10)
    void leaderboardReturnsListOrEmpty() {
        given().accept(ContentType.JSON)
               .when().get("/leaderboard")
               .then()
               .statusCode(200)
               .body("$", instanceOf(java.util.List.class));
    }

    @Test
    @Order(11)
    void postScoreWithoutWinReturnsConflict() {
        given().contentType(ContentType.JSON).post("/game/start").then().statusCode(200);

        given().contentType(ContentType.JSON)
               .body("{\"name\": \"Alice\"}")
               .when().post("/leaderboard")
               .then()
               .statusCode(409);
    }

    @Test
    @Order(12)
    void postScoreWithBlankNameReturns400() {
        given().contentType(ContentType.JSON)
               .body("{\"name\": \"   \"}")
               .when().post("/leaderboard")
               .then()
               .statusCode(400)
               .body("message", notNullValue());
    }

    @Test
    @Order(13)
    void postScoreWithTooLongNameReturns400() {
        given().contentType(ContentType.JSON)
               .body("{\"name\": \"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\"}")  // 31 chars
               .when().post("/leaderboard")
               .then()
               .statusCode(400);
    }
}
