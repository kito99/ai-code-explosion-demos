# Guess the Number with Duke — Jakarta EE demo

Guess a number from 1 to 10 against Duke, then post your win to a shared
leaderboard. Built from [`BRD.md`](BRD.md) on Open Liberty with Jakarta EE 11 and
MicroProfile 7.1.

## How this was generated

| | |
| --- | --- |
| Date of generation | 2026-10-27 |
| Agent harness | Claude Code |
| Model | Claude Opus 5 (1M context), model ID `claude-opus-5[1m]` |
| Prompt | [`jakarta-ee-prompt.txt`](jakarta-ee-prompt.txt) plus [`demo-prompt-common.md`](demo-prompt-common.md) |

## Lines of code

Front-end code and image assets are left out of both counts, so the numbers
compare across demos built on different stacks. Run both commands from this
directory.

Application code — Java, the Maven build, the Liberty and persistence
configuration, and `compose.yaml`:

```bash
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml compose.yaml
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            34            197            110            849
Maven                            1             16              8            142
XML                              3             10              8             60
YAML                             1              1              0             18
Properties                       1              1              5              7
-------------------------------------------------------------------------------
SUM:                            40            225            131           1076
-------------------------------------------------------------------------------
```

Test code:

```bash
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             9            193             20            814
-------------------------------------------------------------------------------
SUM:                             9            193             20            814
-------------------------------------------------------------------------------
```

## Stack

| Piece | Version |
| --- | --- |
| Open Liberty | 26.0.0.10 |
| Jakarta EE | 11 (Web Profile) |
| MicroProfile | 7.1 — OpenAPI, Telemetry 2.1, Health |
| Java | 25 |
| PostgreSQL | 18.6, official Docker image |
| Jakarta Persistence provider | EclipseLink, the one Liberty ships |
| Tests | JUnit 6, Testcontainers |

`server.xml` names the two platforms and then lists features without versions, so
Liberty resolves each one to the version those platforms include.

## Prerequisites

A JDK 25 or later, Maven 3.9 or later, and Docker with Compose. Everything else
downloads on the first build, including the Open Liberty runtime itself.

## Start and stop the database

Run these from this directory:

```bash
docker compose up -d      # start PostgreSQL
docker compose down       # stop it, keeping the data
docker compose down -v    # stop it and throw the data away
```

## Ports

| What | Port |
| --- | --- |
| Application, HTTP | 9080 |
| Application, HTTPS | 9443 |
| PostgreSQL | 5433 on the host, 5432 inside the container |

The database sits on 5433 rather than the usual 5432 so this demo can run
alongside another one that took the default port.

## Database defaults

| Setting | Value |
| --- | --- |
| Database | `guessdb` |
| User | `guess` |
| Password | `guess` |

`compose.yaml` and `server.xml` carry the same values, so a fresh clone runs with
no setup. Each is a Liberty variable with a default, so an environment variable of
the same name — `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` —
overrides it without touching the configuration.

## How the schema gets created

Jakarta Persistence generates it from the mapped `LeaderboardEntry` class. The
persistence unit in `src/main/resources/META-INF/persistence.xml` asks EclipseLink
for `create-or-extend-tables`, so the `leaderboard_entry` table appears the first
time the application touches the database and an existing one is left alone on
later starts. There is no migration step and no `schema.sql`.

## Build, test, and run

Every command runs from this directory. This repository holds one directory per
demo and no build file of its own, so running them from the repository root fails.

```bash
# Unit tests only — no Docker needed
mvn test

# Everything: unit tests, then the integration tests against a real server
mvn verify

# Run the app (start the database first)
docker compose up -d
mvn liberty:run

# Run with hot reload while editing
mvn liberty:dev
```

`mvn liberty:run` holds the terminal and streams the server log to it. Stop it with
Ctrl+C. To run the server in the background instead:

```bash
mvn package
./target/liberty/wlp/bin/server start guessTheNumberServer
./target/liberty/wlp/bin/server stop guessTheNumberServer
```

### What the tests do

`mvn test` runs the unit tests: the game rules, the name rules, the secret number
draw, and the wording of every player-facing message.

`mvn verify` adds an integration test that plays the real application over HTTP. It
starts its own PostgreSQL container through Testcontainers and points a Liberty
server at it, so nothing has to be running first — Docker is the only requirement.
It plays games through to a win and to a loss, posts scores, checks the ranking and
the ten-entry limit, reads the OpenAPI document, looks for the exported telemetry in
the server log, and restarts the server to show that leaderboard entries survive it.

## URLs

| Screen | URL |
| --- | --- |
| Game | <http://localhost:9080/> |
| Leaderboard | <http://localhost:9080/leaderboard.html> |
| OpenAPI document | <http://localhost:9080/openapi> |
| API documentation | <http://localhost:9080/openapi/ui> |
| Health | <http://localhost:9080/health> |

## The HTTP API

A game belongs to the caller's HTTP session, so the browser's session cookie is what
ties a guess to a game. The server takes the winning guess count from the game it
remembers rather than from the request, so a caller cannot claim a better score than
it played.

| Method and path | What it does |
| --- | --- |
| `POST /api/game` | Start a new game |
| `GET /api/game` | Read the current game |
| `POST /api/game/guesses` | Submit a guess |
| `GET /api/leaderboard` | Read the top ten wins |
| `POST /api/leaderboard` | Post the session's win under a name |

## Telemetry

MicroProfile Telemetry exports traces, metrics, and logs to the console, so nothing
else has to be running to see them. `src/main/liberty/config/bootstrap.properties`
turns the SDK on and picks the console exporter for all three signals.

With `mvn liberty:run` the telemetry appears in the terminal. Otherwise it is in the
server log:

```bash
tail -f target/liberty/wlp/usr/servers/guessTheNumberServer/logs/console.log
```

Liberty starts a fresh `console.log` on every server start. The same output also
accumulates in `messages.log` next to it.

What is there:

* **Traces** — a span for every HTTP request, carrying the route, method, and status
  code, plus application spans named `game.start`, `game.guess`, `leaderboard.record`,
  and `leaderboard.read`.
* **Metrics** — `game.started`, `game.won`, `game.lost`, `game.guess.accepted`,
  `game.guess.rejected`, `leaderboard.score.recorded`, `api.error`, and a
  `game.winning.guesses` histogram, alongside Liberty's own request duration metrics.
  They are reported every ten seconds.
* **Logs** — the application's own messages arrive as OpenTelemetry log records with
  the trace and span IDs attached, so a log line can be matched to the request that
  produced it.

No player name reaches any of it. The only personal data the app stores is the name
on the leaderboard, and it appears nowhere else.

### Sending it to a real collector

`compose.yaml` has no collector in it on purpose. To point the app at one, change the
exporters in `src/main/liberty/config/bootstrap.properties`:

```properties
otel.traces.exporter=otlp
otel.metrics.exporter=otlp
otel.logs.exporter=otlp
otel.exporter.otlp.endpoint=http://localhost:4317
```

The same properties can be supplied as environment variables instead
(`OTEL_TRACES_EXPORTER`, `OTEL_EXPORTER_OTLP_ENDPOINT`, and so on), which changes the
destination without touching a file in the build.

## Clearing the leaderboard

The app has no way to delete entries, so clear them in the database. Either wipe
everything:

```bash
docker compose down -v && docker compose up -d
```

Or empty just the table and keep the container running:

```bash
docker compose exec leaderboard-db psql -U guess -d guessdb -c 'DELETE FROM leaderboard_entry;'
```

## Layout

| Path | What is in it |
| --- | --- |
| `src/main/java/.../game` | The game itself: rules, a round, the session's current game |
| `src/main/java/.../leaderboard` | The entity, its repository, and the name rules |
| `src/main/java/.../api` | Jakarta REST resources, request and response records, error mapping |
| `src/main/java/.../telemetry` | The OpenTelemetry counters and histogram |
| `src/main/liberty/config` | `server.xml` and `bootstrap.properties` |
| `src/main/webapp` | The two screens, their stylesheet and scripts, and Duke |
| `src/test/java/.../it` | The integration test and the server it drives |

## Duke

The two Duke drawings are the official ones from the
[OpenJDK Duke project](https://github.com/openjdk/duke) — `vector/Thinking.svg` and
`vector/Wave.svg`. The only change is a tighter `viewBox` so each one crops to the
drawing instead of a letter-size page; the artwork is untouched.
