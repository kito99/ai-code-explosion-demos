# Guess the Number with Duke

Spring Boot 4.1 / Java 25 implementation of `BRD.md`. Prompt: `java-prompt.txt`; shared instructions: `demo-prompt-common.md`.

| | |
|---|---|
| Date of generation | 2026-10-28 |
| Agent harness | GitHub Copilot CLI 1.0.103 |
| Model | Claude Sonnet 5.5 (`claude-sonnet-5.5`) |

## Lines of code

Generated with cloc 2.06, run from this directory:

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            29            124             27            564
Maven                            1              6              0             86
YAML                             1              5              3             44
SQL                              1              1              0              8
-------------------------------------------------------------------------------
SUM:                            32            136             30            702
```

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             6             62              2            260
-------------------------------------------------------------------------------
SUM:                             6             62              2            260
```

## Prerequisites

Java 25, Maven 3.9+, Docker.

## Supporting services

All commands run from this directory (`java-demo-copilot-cli-sonnet-5-5_1/`).

```
docker compose up -d      # start PostgreSQL 18.1
docker compose down       # stop (data kept in the duke-pgdata volume)
docker compose down -v    # stop and delete the data
```

| Service | Port | Notes |
|---|---|---|
| App | 8080 | override with `SERVER_PORT` |
| PostgreSQL | 5432 | host port override with `DB_PORT` (compose) and `SPRING_DATASOURCE_URL` (app) |

Database defaults: name `dukedb`, user `duke`, password `duke`.

The schema is created on startup from `src/main/resources/schema.sql` (`CREATE TABLE IF NOT EXISTS`, `spring.sql.init.mode=always`), so a fresh database needs no manual step.

## Build, test, run

From this directory:

```
mvn clean verify                                         # build and test; tests start their own PostgreSQL via Testcontainers (Docker required)
docker compose up -d                                     # database for running the app
java -jar target/java-demo-copilot-cli-sonnet-5-5_1-1.0.0.jar
```

During development: `mvn spring-boot:run`.

## URLs

- Game: <http://localhost:8080/>
- Leaderboard: <http://localhost:8080/leaderboard.html>
- Interactive API docs (Swagger UI): <http://localhost:8080/swagger-ui.html>
- OpenAPI definition: <http://localhost:8080/v3/api-docs>

## Telemetry

Spans (via the OpenTelemetry tracing bridge) and metrics (Micrometer) are written to the app's own log by default; logs are structured JSON (ECS) with trace and span ids. Look for:

- `'duke.game.start'`, `'duke.game.guess'`, `'duke.game.post-score'` spans, plus `http.server.requests` spans for every API request.
- Metrics snapshots every 30 s: `duke.games.started`, `duke.games.won`, `duke.games.lost`, `duke.guesses{outcome}`, `duke.scores.posted`, `http.server.requests`.

Player names are never written to telemetry.

To send telemetry to a collector instead (or as well), set in `application.yaml` or via environment:

- Traces: `management.opentelemetry.tracing.export.otlp.endpoint=http://collector:4318/v1/traces`
- Metrics: `management.otlp.metrics.export.enabled=true` and `management.otlp.metrics.export.url=http://collector:4318/v1/metrics`
- Turn the console exporters off with `duke.telemetry.console-traces=false` / `duke.telemetry.console-metrics=false`.

The service name is `java-demo-copilot-cli-sonnet-5-5_1` (`spring.application.name`).

## Clear the leaderboard

The app cannot delete entries. From any directory:

```
docker exec java-demo-copilot-cli-sonnet-5-5_1-db psql -U duke -d dukedb -c "DELETE FROM leaderboard_entry"
```

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/games` | Start a game |
| POST | `/api/games/{gameId}/guesses` | Make a guess (`{"guess": 1-10}`) |
| POST | `/api/games/{gameId}/score` | Post a won game (`{"name": "..."}`) |
| GET | `/api/leaderboard` | Top 10, fewest guesses first, ties by earliest |

Games live in memory and expire after 30 minutes idle (`duke.game.session-timeout`); the secret number never leaves the server until a game is lost. Leaderboard entries persist in PostgreSQL.
