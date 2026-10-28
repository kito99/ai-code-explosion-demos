package com.example.guessduke.web;

import java.util.Objects;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// Serves the game screen's requests: starting a round, guessing, and posting a win.
@RestController
@RequestMapping(path = "/api/game", produces = MediaType.APPLICATION_JSON_VALUE)
public class GameController {

    private final GameplayService gameplayService;
    private final ScorePostingService scorePostingService;
    private final GameSessionStore gameSessionStore;

    public GameController(final GameplayService gameplayService,
                          final ScorePostingService scorePostingService,
                          final GameSessionStore gameSessionStore) {
        this.gameplayService = Objects.requireNonNull(gameplayService, "gameplayService");
        this.scorePostingService = Objects.requireNonNull(scorePostingService, "scorePostingService");
        this.gameSessionStore = Objects.requireNonNull(gameSessionStore, "gameSessionStore");
    }

    /// Starts a fresh round, discarding any round the session already held.
    @PostMapping
    public GameView startGame(final HttpSession session) {
        return gameplayService.welcome(gameSessionStore.hold(session, gameplayService.newGame()));
    }

    /// Scores a guess against the round in progress. A session that lost its round
    /// gets a fresh one instead, and the guess does not count against it.
    @PostMapping(path = "/guesses", consumes = MediaType.APPLICATION_JSON_VALUE)
    public GameView submitGuess(final HttpSession session, @RequestBody final GuessRequest request) {
        return gameSessionStore.find(session)
                .map(game -> gameplayService.submitGuess(game, request.guess()))
                .orElseGet(() -> gameplayService.afterLostSession(
                        gameSessionStore.hold(session, gameplayService.newGame())));
    }

    /// Posts the round's win to the leaderboard under the name the player typed.
    @PostMapping(path = "/scores", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ScoreSubmissionView postScore(final HttpSession session, @RequestBody final ScoreRequest request) {
        return gameSessionStore.find(session)
                .map(game -> scorePostingService.postScore(game, request.name()))
                .orElseGet(() -> ScoreSubmissionView.declined(GameMessages.NO_SCORE_TO_POST));
    }
}
