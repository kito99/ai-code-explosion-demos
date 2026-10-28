package com.example.duke;

import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GameController {
    private static final String GAME_KEY = "game";

    private final LeaderboardRepository leaderboard;
    private final RandomGenerator random;

    public GameController(LeaderboardRepository leaderboard, RandomGenerator random) {
        this.leaderboard = leaderboard;
        this.random = random;
    }

    @PostMapping("/game")
    public Map<String, Object> newGame(HttpSession session) {
        var game = new Game(random);
        session.setAttribute(GAME_KEY, game);
        return view("ACTIVE", "Duke is thinking of a number from 1 to 10. You have 5 guesses. Enter your first guess!", game);
    }

    @PostMapping("/game/guess")
    public ResponseEntity<Map<String, Object>> guess(@RequestBody(required = false) Map<String, Object> body,
                                                     HttpSession session) {
        var game = (Game) session.getAttribute(GAME_KEY);
        if (game == null) {
            return error(HttpStatus.CONFLICT, "NO_GAME", "There is no game in progress. Choose Play again to start one.");
        }
        synchronized (game) {
            if (game.state() != Game.State.ACTIVE) {
                return error(HttpStatus.CONFLICT, "GAME_OVER", "This game is over. Choose Play again to start a new one.");
            }
            Integer value = parseGuess(body == null ? null : body.get("guess"));
            if (value == null) {
                return ResponseEntity.ok(view("INVALID",
                        "Please enter a whole number from 1 to 10. That didn't count as a guess, so try again!", game));
            }
            var outcome = game.guess(value);
            return ResponseEntity.ok(switch (outcome) {
                case CORRECT -> view("WON", "Congratulations! You guessed it in " + game.guessesMade()
                        + (game.guessesMade() == 1 ? " guess" : " guesses")
                        + ". Enter your name to post your score, or play again.", game);
                case TOO_LOW -> game.state() == Game.State.LOST ? lost(game, value, "low")
                        : view("TOO_LOW", value + " is too low. Try a higher number!", game);
                case TOO_HIGH -> game.state() == Game.State.LOST ? lost(game, value, "high")
                        : view("TOO_HIGH", value + " is too high. Try a lower number!", game);
            });
        }
    }

    @PostMapping("/game/score")
    public ResponseEntity<Map<String, Object>> postScore(@RequestBody(required = false) Map<String, Object> body,
                                                         HttpSession session) {
        var game = (Game) session.getAttribute(GAME_KEY);
        if (game == null) {
            return error(HttpStatus.CONFLICT, "NO_GAME", "There is no game in progress. Choose Play again to start one.");
        }
        synchronized (game) {
            if (game.state() != Game.State.WON) {
                return error(HttpStatus.CONFLICT, "NOT_WON", "Only a winning game can post a score. Play again to try for a win!");
            }
            if (game.scorePosted()) {
                return error(HttpStatus.CONFLICT, "ALREADY_POSTED", "Your score is already on the leaderboard. Choose Play again to try for a better one!");
            }
            Object raw = body == null ? null : body.get("name");
            String name = NameValidator.normalize(raw instanceof String s ? s : null);
            if (name == null) {
                return ResponseEntity.ok(view("INVALID_NAME",
                        "Please enter a name with at least one visible character and no more than 30 characters.", game));
            }
            leaderboard.add(name, game.guessesMade());
            game.markScorePosted();
            return ResponseEntity.ok(view("POSTED",
                    "Your score is on the leaderboard, " + name + "! View the leaderboard or play again.", game));
        }
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard() {
        return leaderboard.top();
    }

    private Map<String, Object> lost(Game game, int value, String direction) {
        var m = view("LOST", value + " is too " + direction + ". You're out of guesses! Duke was thinking of "
                + game.secret() + ". Play again to give it another try.", game);
        m.put("secret", game.secret());
        return m;
    }

    private static Integer parseGuess(Object raw) {
        int value;
        if (raw instanceof Integer i) {
            value = i;
        } else if (raw instanceof String s && s.strip().matches("[+-]?\\d{1,9}")) {
            value = Integer.parseInt(s.strip());
        } else {
            return null;
        }
        return value >= Game.MIN && value <= Game.MAX ? value : null;
    }

    private static Map<String, Object> view(String status, String message, Game game) {
        var m = new LinkedHashMap<String, Object>();
        m.put("status", status);
        m.put("message", message);
        m.put("guessesMade", game.guessesMade());
        m.put("guessesRemaining", game.guessesRemaining());
        return m;
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        var m = new LinkedHashMap<String, Object>();
        m.put("status", code);
        m.put("message", message);
        return ResponseEntity.status(status).body(m);
    }
}
