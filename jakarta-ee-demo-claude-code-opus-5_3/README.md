# Guess the Number with Duke — Jakarta EE demo

A single-player guessing game built from `BRD.md` on Open Liberty with Jakarta EE 11
and MicroProfile 7.1. Duke thinks of a number from 1 to 10, you get 5 guesses, and
wins can go on a shared leaderboard.

## Generation

| | |
| --- | --- |
| Date generated | 2026-10-28 |
| Agent harness | Claude Code |
| Model | Claude Opus 5 (1M context), `claude-opus-5[1m]` |

## Lines of code

Counted with cloc 2.06, excluding front-end code and assets.

Application code — Java, Maven, and Liberty server configuration:

```
$ cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            32            151             58            574
Maven                            1             13              3            225
XML                              2              7              1             29
-------------------------------------------------------------------------------
SUM:                            35            171             62            828
-------------------------------------------------------------------------------
```

Test code:

```
$ cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             7            125             10            534
-------------------------------------------------------------------------------
SUM:                             7            125             10            534
-------------------------------------------------------------------------------
```

## Requirements

* JDK 25 or later
* Maven 3.9 or later

Open Liberty 26.0.0.10 and the Derby JDBC driver are downloaded by the build. Nothing
else has to be installed or started: the leaderboard lives in an embedded, in-memory
Derby database inside the server process, so there is no database to run alongside it.

## Build, test, run

Every command runs from this directory
(`jakarta-ee-demo-claude-code-opus-5_3`), not from the repository root — the root has
no build file of its own.

Build and run every test, starting and stopping Liberty automatically:

```
mvn verify
```

Unit tests only:

```
mvn test
```

Run the app, rebuilding and redeploying as you edit:

```
mvn liberty:dev
```

Press `Ctrl+C` to stop it. To run the server in the background instead:

```
mvn package -DskipITs liberty:start   # start
mvn liberty:stop                      # stop
```

## URLs and ports

The server listens on HTTP port **9082** and HTTPS port **9445**. Change them with
`-Dhttp.port=...` and `-Dhttps.port=...`, or edit the properties in `pom.xml`.

| Screen or endpoint | URL |
| --- | --- |
| Game | http://localhost:9082/ |
| Leaderboard | http://localhost:9082/leaderboard.html |
| Game REST API | http://localhost:9082/api/game |
| Leaderboard REST API | http://localhost:9082/api/leaderboard |
| Readiness check | http://localhost:9082/health/ready |
| Metrics | http://localhost:9082/metrics |

## Clearing the leaderboard

The app never deletes entries, and the leaderboard lives in an in-memory database, so
restarting the server clears it:

```
mvn liberty:stop
mvn package -DskipITs liberty:start
```

Under `mvn liberty:dev`, stopping and restarting dev mode does the same.

## How it fits together

* **Game state** is per browser session. A `@SessionScoped` CDI bean holds the current
  `GuessingGame`, so Duke's secret number never reaches the browser until the game ends.
* **The REST API** is Jakarta REST with JSON-B. Guesses arrive as the text the player
  typed, so anything that is not a whole number from 1 to 10 comes back as a friendly
  message without counting against the guess limit.
* **The leaderboard** is a Jakarta Data repository over a Jakarta Persistence entity,
  ranked by fewest guesses with ties broken by which result was recorded first.
* **Validation** is Jakarta Validation on the stored entry plus a `PlayerName` type that
  trims and checks names, with exception mappers turning every rejection into friendly
  JSON.
* **MicroProfile** supplies a readiness check over the leaderboard store and a counter
  of games started.
* **The front end** is plain HTML, CSS, and JavaScript. Duke comes from the official
  `openjdk/duke` repository: the Thinking pose while you play, and the Wave pose for the
  win, which springs up and settles back down three times over 1.5 seconds.

## Tests

`mvn verify` runs both suites on a clean machine with nothing started by hand:

* **Unit tests** cover the game rules, the random draw, name validation, and leaderboard
  ranking.
* **Integration tests** (`GuessTheNumberIT`) drive the running server over HTTP: winning
  and posting a score, posting a win only once, rejecting blank names and bad guesses,
  losing and revealing the secret number, starting over, two players sharing the
  leaderboard but not a game, the screens and Duke images being served, and a run of
  twelve wins that checks the leaderboard really does stop at ten and rank them by
  fewest guesses then by who posted first.

The browser itself is not driven by a test. The integration tests cover the API the
screens call, and the screens were checked by hand.
