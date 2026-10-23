# Guess the Number with Duke

A small web game built against [BRD.md](BRD.md). Duke, the Java mascot, thinks of a
whole number between 1 and 10, and you get five tries to find it. Guess right and
Duke does a happy little jump.

It is plain Java on purpose: the game runs on the JDK's built-in
`com.sun.net.httpserver.HttpServer`, with no web framework, no servlet container,
and no runtime dependencies. The front end is hand-written HTML, CSS, and
JavaScript, and Duke himself is an inline SVG so CSS can animate him.

## Running it

```bash
mvn package
java -jar target/java-demo-1.jar
```

Then open <http://localhost:8080/>.

Pass a port as the first argument or set the `PORT` environment variable to use a
different one:

```bash
java -jar target/java-demo-1.jar 9000
PORT=9000 java -jar target/java-demo-1.jar
```

During development you can skip the packaging step:

```bash
mvn compile exec:java
```

## Running the tests

```bash
mvn test
```

The suite covers the game rules directly and then plays whole rounds over HTTP
against a real server, so the wiring is tested along with the logic.

## How it fits together

| Class | What it does |
| --- | --- |
| `App` | Starts the HTTP server, wires up the two handlers, sweeps idle sessions. |
| `Game` | Every game rule: the secret, the five-guess limit, validation, and the player-facing messages. |
| `GameState` | An immutable snapshot of a game, serialized to JSON for the browser. |
| `GameStore` | Holds one game per player, keyed by a session cookie. |
| `ApiHandler` | The JSON API. |
| `StaticHandler` | Serves the front end out of the jar. |
| `Json` | A few lines of JSON writing, so there are no dependencies. |

The game state lives on the server. The browser never learns the secret number
until the game is over, and the win and lose decisions are the server's to make.

### API

| Request | Response |
| --- | --- |
| `GET /api/game` | The player's current game, starting one if they have none. |
| `POST /api/game/new` | Abandons the current game and starts a fresh one. |
| `POST /api/game/guess` | Scores one guess, sent as the form field `guess`. |

All three return the same JSON shape:

```json
{
  "status": "PLAYING",
  "guessesMade": 2,
  "guessesRemaining": 3,
  "maxGuesses": 5,
  "minNumber": 1,
  "maxNumber": 10,
  "lastGuess": 4,
  "hint": "TOO_LOW",
  "message": "4 is too low. Try a higher number — 3 guesses left.",
  "guessAccepted": true,
  "secret": null
}
```

`status` is `PLAYING`, `WON`, or `LOST`. `secret` stays `null` until the player
loses, which is the only time the game reveals it.

## Notes on the requirements

- **Duke's celebration (FR-10)** is CSS only: he springs up and settles three
  times over 1.5 seconds while waving, then stops in his resting pose. It runs
  once and never blocks Play again, and it is switched off entirely for anyone
  who asks for reduced motion — the congratulations message still shows.
- **Validation (FR-6)** happens on the server, and only on the server. The
  input field does not cap its own length, because truncating what you typed
  could quietly turn a typo like `100` into a perfectly valid guess of `10`.
  Anything that is not a whole number from 1 to 10 gets a friendly nudge and
  does not cost a guess.
- **Play again (FR-12)** stays live even while a guess is still in flight. A
  request that hangs cannot wedge the game: the restart supersedes it, and the
  stale reply is dropped instead of overwriting the new round.
- **Accessibility (NFR-3)** — the whole game is playable from the keyboard,
  focus follows the round (into the input while playing, onto Play again when it
  ends), messages live in an `aria-live` region, and every piece of game text
  clears WCAG AA contrast.
