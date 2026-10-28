# Guess the Number with Duke

A small web game built from [`BRD.md`](BRD.md). Duke thinks of a whole number from
1 to 10, you get five guesses, and a win can go on a leaderboard that outlives
the app process.

Every command below runs from this directory
(`java-demo-claude-code-opus-5_2`). The repository root holds one directory per
demo and no build file of its own, so commands run from there will fail.

## How this demo was generated

| | |
| --- | --- |
| Date of generation | 2026-10-28 |
| Agent harness | Claude Code |
| Model | Claude Opus 5 (1M context), model id `claude-opus-5[1m]` |
| Prompt | [`java-prompt.txt`](java-prompt.txt) plus [`demo-prompt-common.md`](demo-prompt-common.md) |
| Requirements | [`BRD.md`](BRD.md) |

## Stack

Spring Boot 4.1.2 on Java 25: Spring MVC, `JdbcClient` over PostgreSQL 18 with
Flyway migrations, springdoc-openapi 3.2.0 for the API definition and Swagger UI,
and Spring Boot's OpenTelemetry starter for traces and metrics. The front end is
plain HTML, CSS, and JavaScript served as static files. Tests use JUnit Jupiter
6.0.3, AssertJ, Mockito, and Testcontainers 2.0.5.

The server holds the secret number and the guess count for each round, so a
posted score always reflects the game the server scored rather than something the
browser claimed. Rounds live in memory, and one that has been in play for over two
hours is swept the next time a game starts; only leaderboard entries go to the
database.

## Prerequisites

A JDK 25 or newer, Maven, and Docker with Compose. The test suite starts its own
PostgreSQL container, so nothing has to be running before `mvn test`.

## Build, test, and run

```bash
# Build without running tests
mvn -DskipTests package

# Run the full suite; Testcontainers starts PostgreSQL 18 on its own
mvn test

# Run the app (the database below has to be up)
mvn spring-boot:run

# Or run the packaged jar
java -jar target/java-demo-claude-code-opus-5_2.jar
```

`mvn spring-boot:run` stops with Ctrl-C.

## Supporting services

```bash
# Start PostgreSQL
docker compose up -d

# Stop it, keeping the data
docker compose stop

# Stop it and throw the data away
docker compose down -v
```

## Ports, database, and credentials

| | |
| --- | --- |
| App | <http://localhost:8081> |
| Database | `localhost:5434` |
| Database name | `dukedb` |
| User | `duke` |
| Password | `duke` |

The app defaults to exactly these values, so a fresh clone runs with
`docker compose up -d` and nothing else. The ports sit clear of the other demos
in this repository; to change them, edit `compose.yaml` and
`src/main/resources/application.yaml` together.

## How the schema gets created

Flyway runs `src/main/resources/db/migration/V1__create_leaderboard_entry.sql`
at startup, so a fresh database ends up with the `leaderboard_entry` table on
first start with no manual step. An existing database keeps its rows: Flyway
records what it has already applied and skips it on later starts.

## URLs

| What | Where |
| --- | --- |
| Game | <http://localhost:8081/> |
| Leaderboard screen | <http://localhost:8081/leaderboard.html> |
| OpenAPI definition | <http://localhost:8081/v3/api-docs> |
| API documentation (Swagger UI) | <http://localhost:8081/swagger-ui.html> |
| Health and metrics | <http://localhost:8081/actuator/health>, <http://localhost:8081/actuator/metrics> |

The API is four endpoints: `POST /api/games` starts a round,
`POST /api/games/{gameId}/guesses` submits a guess,
`POST /api/games/{gameId}/score` posts a win, and `GET /api/leaderboard` reads
the standings. Errors come back as RFC 9457 problem documents whose `detail` is
the sentence the player sees, alongside a machine-readable `code` so an API
consumer can tell a rejected guess from a finished game without matching on
message text.

## Telemetry

Traces, metrics, and structured logs all land in the app's own log, so the game
is observable with nothing else running.

* **Traces** go to the OpenTelemetry `LoggingSpanExporter`. Every API request
  produces a server span, and the game operation behind it produces a child span
  named `duke.game.start`, `duke.game.guess`, `duke.game.post-score`, or
  `duke.leaderboard.read`, tagged with the verdict and the outcome.
* **Metrics** go to Micrometer's `LoggingMeterRegistry` every 30 seconds. The
  game meters are `duke.games.started`, `duke.games.finished` (tagged
  `outcome=won|lost`), `duke.guesses.accepted` (tagged with the verdict),
  `duke.guesses.rejected`, `duke.wins.guesses`, `duke.scores.posted`, and
  `duke.games.in-play`, alongside a timer per operation above and the usual
  `http.server.requests` timings.
* **Logs** are ECS JSON on the console, each line carrying the trace and span id
  of the request that wrote it.

To watch it while playing:

```bash
mvn spring-boot:run | grep -E 'duke\.|http\.server\.requests'
```

Player names never reach telemetry. Logs, spans, and meter tags carry game ids,
guess counts, verdicts, and outcomes only.

### Pointing it at a real collector

No collector is in `compose.yaml`; the destination is configuration, not code.
With an OTLP collector listening on `http://localhost:4318`:

| Signal | Property | Value |
| --- | --- | --- |
| Traces | `management.opentelemetry.tracing.export.otlp.endpoint` | `http://localhost:4318/v1/traces` |
| Metrics | `management.otlp.metrics.export.enabled` | `true` |
| Metrics | `management.otlp.metrics.export.url` | `http://localhost:4318/v1/metrics` |

Set them in `src/main/resources/application.yaml` or pass them on the command
line, for example

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments=--management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces
```

Console export is independent of all this and can be turned off with
`duke.telemetry.console-traces=false` and `duke.telemetry.console-metrics=false`.
Logs are structured on the console rather than shipped over OTLP; sending them to
a collector as well would mean adding the OpenTelemetry Logback appender and
setting `management.opentelemetry.logging.export.otlp.endpoint`.

## When the database is unreachable

The game itself needs no database, so a round keeps playing while PostgreSQL is
down. Posting a score answers `503` with a message saying to try again shortly,
and the win stays postable: the server claims the win before writing it and hands
the claim back if the write does not land, so a retry once the database is back
still records it. That claim also settles concurrent posts, so one win can never
produce two entries.

## Clearing the leaderboard

The app has no delete endpoint, by design. Between runs, either empty the table:

```bash
docker compose exec db psql -U duke -d dukedb -c 'TRUNCATE leaderboard_entry;'
```

or throw the whole database away and let it be recreated:

```bash
docker compose down -v && docker compose up -d
```

## Lines of code

Counted with [cloc](https://github.com/AlDanial/cloc) 2.06, excluding front-end
code and image assets.

Application code, meaning the Java sources plus the build and configuration
files:

```
$ cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml compose.yaml
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            41            239            134            914
Maven                            1              6              1             94
YAML                             2              7              7             62
SQL                              1              1              2              8
-------------------------------------------------------------------------------
SUM:                            45            253            144           1078
-------------------------------------------------------------------------------
```

Test code:

```
$ cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            13            239             21            821
-------------------------------------------------------------------------------
SUM:                            13            239             21            821
-------------------------------------------------------------------------------
```

**Application code: 1078. Test code: 821.**

## Duke artwork

The three drawings in `src/main/resources/static/img` come from the
[OpenJDK Duke project](https://github.com/openjdk/duke): `vector/Thinking.svg`
while Duke is thinking of the number, and `vector/Wave.svg` and `vector/Hips.svg`
as the two frames of his celebration wave. Each one keeps its original artwork
and carries a tighter `viewBox` so Duke fills his frame on screen.
