# Guess the Number with Duke

A small web game built from [`BRD.md`](BRD.md): Duke thinks of a number from 1 to
10, you get five guesses, and a win can go on a leaderboard that survives a
restart.

Every command below runs from this directory
(`java-demo-claude-code-opus-5-1`), not from the repository root. The root holds
one directory per demo and no build file of its own.

## How this demo was generated

| | |
| --- | --- |
| Date of generation | 2026-10-27 |
| Agent harness | Claude Code |
| Model | Claude Opus 5 (1M context), model id `claude-opus-5[1m]` |
| Prompt | [`java-prompt.txt`](java-prompt.txt) plus [`demo-prompt-common.md`](demo-prompt-common.md) |
| Requirements | [`BRD.md`](BRD.md) |

## Stack

Spring Boot 4.1.2 on Java 25, with Spring MVC, Spring Data JPA, PostgreSQL 18,
springdoc-openapi 3.2.0, and Spring Boot's OpenTelemetry starter. The front end
is plain HTML, CSS, and JavaScript served as static files. Tests use JUnit 5 and
Testcontainers 2.0.5.

## Prerequisites

A JDK 25 or newer, Maven, and Docker with Compose. The test suite starts its own
PostgreSQL container, so nothing has to be running before `mvn test`.

## Build, test, and run

```bash
# Build without running tests
mvn -DskipTests package

# Run the full suite; Testcontainers starts PostgreSQL 18 on its own
mvn test

# Start the app (needs the database below to be up)
mvn spring-boot:run
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
| App | <http://localhost:8080> |
| Database | `localhost:5432` |
| Database name | `dukedb` |
| User | `duke` |
| Password | `duke` |

The app defaults to exactly these values, so a fresh clone runs with
`docker compose up -d` and nothing else. Another demo already using 5432 or 8080
has to be stopped first, or these ports changed in `compose.yaml` and
`src/main/resources/application.yaml`.

## How the schema gets created

Hibernate generates it from the mapped `LeaderboardEntry` class:
`spring.jpa.hibernate.ddl-auto` is set to `update`, so a fresh database ends up
with the `leaderboard_entry` table on first start with no manual step, and an
existing database keeps its rows across restarts.

## URLs

| What | Where |
| --- | --- |
| Game | <http://localhost:8080/> |
| Leaderboard screen | <http://localhost:8080/leaderboard.html> |
| OpenAPI definition | <http://localhost:8080/v3/api-docs> |
| API documentation (Swagger UI) | <http://localhost:8080/swagger-ui.html> |
| Health and metrics | <http://localhost:8080/actuator/health>, <http://localhost:8080/actuator/metrics> |

The API is four endpoints: `POST /api/games` starts a round,
`POST /api/games/{gameId}/guesses` submits a guess,
`POST /api/games/{gameId}/score` posts a win, and `GET /api/leaderboard` reads
the standings. The server keeps the secret number and the guess count, so a
posted score always reflects the game the server saw.

## Telemetry

Traces, metrics, and structured logs all land in the app's own log, so the game
is observable with nothing else running.

* **Traces** go to the OpenTelemetry `LoggingSpanExporter`. Every API request
  produces a server span, and the service methods behind it
  (`start-game`, `submit-guess`, `post-score`, `record-score`,
  `read-leaderboard`) produce child spans.
* **Metrics** go to Micrometer's `LoggingMeterRegistry` every 30 seconds. The
  game meters are `duke.games.started`, `duke.games.finished` (tagged
  `outcome=won|lost`), `duke.guesses.accepted` (tagged with the outcome),
  `duke.guesses.rejected`, `duke.wins.guesses`, `duke.scores.posted`, and
  `duke.games.active`, alongside the usual `http.server.requests` timings.
* **Logs** are ECS JSON on the console, each line carrying the trace and span id
  of the request that wrote it.

To watch it while playing:

```bash
mvn spring-boot:run | grep -E 'LoggingSpanExporter|LoggingMeterRegistry'
```

Player names are never written to telemetry. Logs and spans carry game ids,
guess counts, and outcomes only.

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
`mvn spring-boot:run -Dspring-boot.run.arguments=--management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces`.
Console export is independent and can be turned off with
`duke.telemetry.console-traces=false` and `duke.telemetry.console-metrics=false`.

Logs are structured on the console rather than shipped over OTLP; sending them
to a collector as well would mean adding the OpenTelemetry Logback appender and
setting `management.opentelemetry.logging.export.otlp.endpoint`.

## When the database is unreachable

The game itself needs no database, so a round keeps playing while PostgreSQL is
down. Posting a score answers `503` with a message saying to try again shortly,
and the win stays postable: the server claims the win before writing it and
gives the claim back if the write does not land, so a retry after the database
comes back still records it. Concurrent posts of the same win are settled by
that claim, so one win can never produce two entries.

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
Java                            36            190            121            762
Maven                            1             10              2             94
YAML                             2              6              7             72
-------------------------------------------------------------------------------
SUM:                            39            206            130            928
-------------------------------------------------------------------------------
```

Test code:

```
$ cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            11            156              4            651
-------------------------------------------------------------------------------
SUM:                            11            156              4            651
-------------------------------------------------------------------------------
```

**Application code: 928. Test code: 651.**

## Duke artwork

The three images in `src/main/resources/static/img` come from the
[OpenJDK Duke project](https://github.com/openjdk/duke): `png/Duke.Hips.3D.png`
for his resting pose and `png/Duke.Wave1.3D.png` and `png/Duke.Wave2.3D.png` for
the two frames of his celebration wave. They are scaled down to 520 pixels tall
and otherwise unchanged.
