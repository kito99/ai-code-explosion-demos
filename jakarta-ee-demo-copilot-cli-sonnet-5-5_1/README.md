# Guess the Number with Duke (Jakarta EE)

Guess Duke's secret number (1–10) in five tries; winners post their score to a shared leaderboard. Built from `BRD.md` with Open Liberty, Jakarta EE 11 and MicroProfile 7.1.

| | |
| --- | --- |
| Date of generation | 2026-10-28 |
| Agent harness | GitHub Copilot CLI |
| Model | Claude Sonnet 5.5 |
| Prompts | `jakarta-ee-prompt.txt`, `demo-prompt-common.md`, `BRD.md` (copied here) |

## Stack

Jakarta REST, CDI, Validation, Persistence (EclipseLink on PostgreSQL 18.6), JSON-B; MicroProfile OpenAPI, Telemetry, Config, Health. Plain HTML/CSS/JS front-end. Duke artwork (PNG) comes from <https://github.com/openjdk/duke>.

## Lines of code

Generated with `cloc` 2.06.

Application code — run from `jakarta-ee-demo-copilot-cli-sonnet-5-5_1/`:

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            34            197            110            849
Maven                            1             16              8            142
XML                              3             10             10             61
Properties                       1              1              5              7
-------------------------------------------------------------------------------
SUM:                            39            224            133           1059
-------------------------------------------------------------------------------
```

Test code — run from `jakarta-ee-demo-copilot-cli-sonnet-5-5_1/`:

```
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

## Prerequisites

JDK 25, Maven 3.9+, Docker (supporting services and tests).

## Supporting services

Start PostgreSQL — from `jakarta-ee-demo-copilot-cli-sonnet-5-5_1/`:

```
docker compose up -d --wait
```

Stop it (add `-v` to also delete data) — from `jakarta-ee-demo-copilot-cli-sonnet-5-5_1/`:

```
docker compose down
```

## Ports and database defaults

| Service | Port |
| --- | --- |
| App (HTTP) | 9080 (`http.port`) |
| App (HTTPS) | 9443 (`https.port`) |
| PostgreSQL (host) | 5433 → container 5432 |

Database `guessdb`, user `guess`, password `guess`. Override with the environment variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. If 5433 is taken, set `DB_PORT` for both `docker compose up` and the app, e.g. `DB_PORT=5543`.

## Schema

`persistence.xml` sets EclipseLink `ddl-generation=create-or-extend-tables` with `deploy-on-startup=true`, so the `leaderboard_entry` table is created when the app deploys against an empty database; an existing table is left alone. No manual step or SQL script.

## Build, test, run

Build, unit tests and integration tests — from `jakarta-ee-demo-copilot-cli-sonnet-5-5_1/`:

```
mvn verify
```

Unit tests run in Surefire. Integration tests (`*IT`) run in Failsafe: they start their own PostgreSQL with Testcontainers and their own Liberty server from `target/liberty`, so no database needs to be running (Docker must be).

Run the app — from `jakarta-ee-demo-copilot-cli-sonnet-5-5_1/`, with the database up:

```
mvn package -DskipTests
target/liberty/wlp/bin/server run guessTheNumberServer
```

Or in dev mode — from `jakarta-ee-demo-copilot-cli-sonnet-5-5_1/`:

```
mvn liberty:dev
```

Stop a server started with `server start` — from `jakarta-ee-demo-copilot-cli-sonnet-5-5_1/`:

```
target/liberty/wlp/bin/server stop guessTheNumberServer
```

## URLs

| | URL |
| --- | --- |
| Game | <http://localhost:9080/> |
| Leaderboard | <http://localhost:9080/leaderboard.html> |
| OpenAPI definition | <http://localhost:9080/openapi> |
| API docs UI | <http://localhost:9080/openapi/ui> |

## Telemetry

Traces, metrics and logs are exported to the console by default and appear in `target/liberty/wlp/usr/servers/guessTheNumberServer/logs/messages.log` (and the terminal when using `server run`). Metrics export every 10 s. Service name: `jakarta-ee-demo-copilot-cli-sonnet-5-5_1`. Game lifecycle counters (started, won, lost) and per-request spans are included; player names are never recorded.

To send telemetry to a collector instead, edit `src/main/liberty/config/bootstrap.properties`:

```
otel.traces.exporter=otlp
otel.metrics.exporter=otlp
otel.logs.exporter=otlp
otel.exporter.otlp.endpoint=http://localhost:4317
```

Environment variables (`OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_TRACES_EXPORTER`, …) also work without a rebuild.

## Clear the leaderboard

From `jakarta-ee-demo-copilot-cli-sonnet-5-5_1/`:

```
docker compose exec leaderboard-db psql -U guess -d guessdb -c "TRUNCATE leaderboard_entry RESTART IDENTITY"
```

Or reset everything with `docker compose down -v`.
