# Guess the Number with Duke

A small web game built to the requirements in [BRD.md](BRD.md). Duke, the Java
mascot, thinks of a whole number from 1 to 10, and you get five guesses to find
it. Guess right and Duke jumps for joy.

The backend is Jakarta EE 11 running on Open Liberty. The front end is plain
HTML, CSS, and JavaScript, with Duke drawn as inline SVG so his celebration can
animate.

## Running it

You need JDK 21 or newer and Maven 3.9 or newer. Then:

```bash
mvn liberty:dev
```

That starts Liberty in dev mode and serves the game at <http://localhost:9080>.
Dev mode watches your files, so edits to Java sources, the front end, or
`server.xml` take effect without a restart. Press `Ctrl-C` to stop.

To build a deployable WAR and run the full test suite:

```bash
mvn verify
```

That compiles, runs the unit tests, packages `target/jakarta-ee-demo-1.war`,
starts Liberty, runs the integration tests against it, and shuts it down again.
Skip the server round trip with `mvn test -DskipITs`.

The HTTP port comes from the `liberty.var.http.port` property in `pom.xml`, so
you can move it without touching `server.xml`:

```bash
mvn liberty:dev -Dliberty.var.http.port=9090
```

## How it fits together

```
src/main/java/tech/virtua/guessthenumber/
  game/            The rules, as plain Java with no container in sight
    GuessingGame     One round: the secret number, the guess limit, the feedback
    GameSnapshot     An immutable read of a game at one moment
    GameStatus       IN_PROGRESS, WON, LOST
    Hint             TOO_LOW, TOO_HIGH, CORRECT
    Acceptance       Whether a submission counted, was invalid, or came too late
  GameSession      The one game belonging to a browser session (@SessionScoped)
  rest/            The HTTP surface
    GameResource     The three endpoints
    GameView         The JSON shape the browser renders from
    GameMessages     The friendly text the player reads

src/main/webapp/   The front end: index.html, css/styles.css, js/app.js
src/main/liberty/config/server.xml   Liberty features and ports
```

The rules live in `GuessingGame`, which knows nothing about HTTP, CDI, or JSON.
That is what lets the bulk of the test suite run in milliseconds without starting
a server. `GameSession` wraps one game per browser session and hands out
immutable snapshots, so two requests racing each other cannot tear the state.
The REST layer turns a snapshot into JSON and picks the sentence the player reads.

The server owns every rule, including what counts as a valid guess. The guess
field is submitted as text rather than a number precisely so the server gets to
decide — the page sends whatever was typed and renders whatever comes back.

Loading the page deals a fresh game, so refreshing always starts you over. The
game itself lives in your HTTP session, which is why `GET /api/game` can hand
back a game already in progress.

## The API

All three endpoints answer with the same JSON shape, so the page renders any
response the same way.

| Request | Answers | Meaning |
| --- | --- | --- |
| `GET /api/game` | `200` | The game in play, starting one if this session is new |
| `POST /api/game` | `200` | Abandons the current game and deals a fresh number |
| `POST /api/game/guesses` | `200` | The guess counted; the body carries the feedback |
| | `400` | Not a whole number from 1 to 10, so it did not count |
| | `409` | The game was already over |

A guess looks like this:

```bash
curl -i -c jar -b jar -X POST http://localhost:9080/api/game/guesses \
  -H 'Content-Type: application/json' -d '{"guess": "7"}'
```

The `-c`/`-b` cookie jar matters: the game lives in your HTTP session, so a
client that drops cookies gets a brand new game on every call.

A response body:

```json
{
  "status": "IN_PROGRESS",
  "guessesMade": 2,
  "guessesRemaining": 3,
  "maxGuesses": 5,
  "minNumber": 1,
  "maxNumber": 10,
  "lastGuess": 9,
  "lastHint": "TOO_HIGH",
  "message": "9 is too high. Try a lower number — you have 3 guesses left."
}
```

`secretNumber` is absent while a game is in progress and appears only once the
game is won or lost. `lastGuess` and `lastHint` are absent until the first
accepted guess.

## Duke's celebration

On a win, the page adds one class to the SVG and CSS keyframes do the rest:
Duke crouches, springs up, and settles through three shrinking bounces over 1.5
seconds while his raised arm waves, then lands back in his resting pose. Nothing
in the app waits on the animation, so Play again stays live the whole time. If
the player has asked their system for reduced motion, the animation is skipped
and the congratulations message stands on its own.

## Tests

```bash
mvn test        # the rules, the session, and the messages — no server needed
mvn verify      # the above, plus the API exercised over HTTP on a real server
```

The unit tests pin the secret number so they can assert exact outcomes. The
integration tests play real games over HTTP with a cookie jar per player, which
covers the pieces unit tests cannot reach: session isolation, the status codes,
JSON binding, and the front end actually being served.
