# Guess the Number with Duke — Jakarta EE demo

Guess a number from 1 to 10 against Duke, then post your win to a shared
leaderboard. Built from [`BRD.md`](BRD.md) on Open Liberty with Jakarta EE 11 and
MicroProfile 7.1.

## How this was generated

| | |
| --- | --- |
| Date of generation | 2026-10-28 |
| Agent harness | Claude Code |
| Model | Claude Opus 5 (1M context), model ID `claude-opus-5[1m]` |
| Prompt | [`jakarta-ee-prompt.txt`](jakarta-ee-prompt.txt) plus [`demo-prompt-common.md`](demo-prompt-common.md) |

## Lines of code

Front-end code and image assets are left out of both counts, so the numbers
compare across demos built on different stacks. Run both commands from this
directory.

Application code — Java, the Maven build, the Liberty configuration, and
`compose.yaml`:

```bash
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml compose.yaml
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            39            266            183           1048
Maven                            1             18             12            150
XML                              2             10             12             45
YAML                             1              1              4             19
Properties                       1              1              5              7
-------------------------------------------------------------------------------
SUM:                            44            296            216           1269
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
Java                             9            190             47            895
-------------------------------------------------------------------------------
SUM:                             9            190             47            895
-------------------------------------------------------------------------------
```

## Stack

| Piece | Version |
| --- | --- |
| Open Liberty | 26.0.0.10 |
| Jakarta EE | 11 — REST, CDI, Persistence, **Data**, Validation, JSON Binding, Servlet |
| MicroProfile | 7.1 — OpenAPI 4.1, Telemetry 2.1 |
| Java | 25 |
| PostgreSQL | 18.6, official Docker image |
| Jakarta Data provider | Liberty's built-in one, backed by EclipseLink |
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
| Application, HTTP | 9081 |
| Application, HTTPS | 9444 |
| PostgreSQL | 5435 on the host, 5432 inside the container |

These are off the usual 9080, 9443, and 5432 so this demo can run alongside the
others in this repository without a port fight.

## Database defaults

| Setting | Value |
| --- | --- |
| Database | `dukeguess` |
| User | `duke` |
| Password | `duke` |

`compose.yaml` and `server.xml` carry the same values, so a fresh clone runs with
no setup. Each is a Liberty variable with a default, so an environment variable of
the same name — `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` —
overrides it without touching the configuration.

## How the schema gets created

Liberty's built-in Jakarta Data provider creates the table from the mapped
`LeaderboardEntry` class the first time a repository method runs, and leaves an
existing table alone afterwards. `server.xml` asks for that explicitly with
`<data createTables="true" dropTables="false"/>`. There is no `persistence.xml`,
no migration step, and no `schema.sql`.

For a database whose user may not create tables, set `createTables="false"` and
generate the DDL with Liberty's `ddlGen` utility instead.

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
./target/liberty/wlp/bin/server start dukeGuessServer
./target/liberty/wlp/bin/server stop dukeGuessServer
```

### What the tests do

`mvn test` runs the unit tests: the game rules, the name rules, the secret number
draw, the wording of every player-facing message, and what each of those turns into
on the way to the screen.

`mvn verify` adds an integration test that plays the real application over HTTP. It
starts its own PostgreSQL container through Testcontainers and points a Liberty
server at it, so nothing has to be running first — Docker is the only requirement.
It plays games through to a win and to a loss, turns away bad guesses and bad
names, posts scores, checks the ranking and the ten-entry limit, reads the OpenAPI
document, looks in the server log for the telemetry its own requests produced, and
restarts the server to show that leaderboard entries survive it.

## URLs

| Screen | URL |
| --- | --- |
| Game | <http://localhost:9081/> |
| Leaderboard | <http://localhost:9081/leaderboard.html> |
| OpenAPI document | <http://localhost:9081/openapi> |
| API documentation | <http://localhost:9081/openapi/ui/> |

## The HTTP API

A game belongs to the caller's HTTP session, so the browser's session cookie is what
ties a guess to a game. The server keeps the secret number and the guess count,
which is why a caller cannot read the answer early or post a better score than it
played. Requests that share a session are serialised, so a double-click cannot spend
the same guess twice or post one win twice.

Jakarta Validation checks the shape of a request — that a guess arrived and that it
is a whole number, and that a name passes the name rules — while whether a guess
falls in the allowed range is the game's own rule. Either way the guess does not
count and the reply explains what to send instead.

| Method and path | What it does |
| --- | --- |
| `POST /api/games` | Start a game |
| `GET /api/games/current` | Read the session's game |
| `POST /api/games/current/guesses` | Take a guess |
| `GET /api/leaderboard` | Read the top ten wins |
| `POST /api/leaderboard` | Post the session's win under a name |

## Telemetry

MicroProfile Telemetry exports traces, metrics, and logs to the console, so nothing
else has to be running to see them. `src/main/liberty/config/bootstrap.properties`
turns the SDK on and picks the console exporter for all three signals.

With `mvn liberty:run` the telemetry appears in the terminal. Otherwise it is in the
server log:

```bash
tail -f target/liberty/wlp/usr/servers/dukeGuessServer/logs/console.log
```

Liberty starts a fresh `console.log` on every server start. The same output also
accumulates in `messages.log` next to it.

What is there:

* **Traces** — a span for every HTTP request, carrying the route, method, and status
  code, plus application spans named `game.start`, `game.guess`, `game.post.score`,
  `leaderboard.record`, and `leaderboard.read`.
* **Metrics** — `game.started`, `game.won`, `game.lost`, `game.guess.accepted`,
  `game.guess.rejected`, `leaderboard.score.recorded`, `api.error` broken down by
  `error.kind`, and a `game.winning.guesses` histogram, alongside Liberty's own
  request duration metrics. They are reported every ten seconds.
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
docker compose exec leaderboard-db psql -U duke -d dukeguess -c 'DELETE FROM leaderboard_entry;'
```

## Layout

| Path | What is in it |
| --- | --- |
| `src/main/java/.../game` | The game itself: rules, a round, the session's current game |
| `src/main/java/.../leaderboard` | The entity, its Jakarta Data repository, and the name rules |
| `src/main/java/.../api` | Jakarta REST resources, request and response records, error mapping |
| `src/main/java/.../telemetry` | The OpenTelemetry counters and histogram |
| `src/main/java/.../error` | The failure kinds the game reports and their base type |
| `src/main/liberty/config` | `server.xml` and `bootstrap.properties` |
| `src/main/webapp` | The two screens, their stylesheet and scripts, and Duke |
| `src/test/java/.../it` | The integration test, the server it drives, and its HTTP client |

## Duke

The two Duke drawings are the official ones from the
[OpenJDK Duke project](https://github.com/openjdk/duke) — `vector/Thinking.svg` and
`vector/Wave.svg`. The only change is a tighter `viewBox` so each one crops to the
drawing instead of a letter-size page; the artwork itself is untouched. Duke thinks
while you guess and switches to the waving drawing for his celebration jump.
