# Guess the Number with Duke

Server-side Java game: guess Duke's number (1–10) in 5 tries, post wins to a shared leaderboard.

| | |
| --- | --- |
| Date of generation | 2026-10-28 |
| Agent harness | GitHub Copilot CLI 1.0.103 |
| Model | Claude Sonnet 5.5 (`claude-sonnet-5.5`) |
| Stack | Java 25, Spring Boot 3.5.16, Spring Data JPA, Flyway, PostgreSQL 18, springdoc-openapi 2.8.17 |

Duke artwork (`Thinking.svg`, `Wave.svg`) comes from [openjdk/duke](https://github.com/openjdk/duke) (BSD-3-Clause).

## Lines of code

Counted with `cloc`, excluding front-end code and assets.

Application code, run from `java-demo-copilot-cli-sonnet-5-5_2/`:

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            30            131              9            497
Maven                            1              6              0             81
YAML                             1              2              0             23
SQL                              1              1              0              7
-------------------------------------------------------------------------------
SUM:                            33            140              9            608
-------------------------------------------------------------------------------
```

Test code, run from `java-demo-copilot-cli-sonnet-5-5_2/`:

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             6             89              2            349
-------------------------------------------------------------------------------
SUM:                             6             89              2            349
-------------------------------------------------------------------------------
```

## Prerequisites

Java 25, Maven 3.9+, Docker (for the database and for tests).

## Supporting services

PostgreSQL 18 runs from `compose.yaml`.

```
# from java-demo-copilot-cli-sonnet-5-5_2/
docker compose up -d      # start
docker compose down       # stop (data kept in the postgres-data volume)
docker compose down -v    # stop and delete the data
```

If host port 5432 is taken, pick another one: `DB_PORT=5433 docker compose up -d`, then start the app with
`DB_URL=jdbc:postgresql://localhost:5433/duke`.

## Ports and database defaults

| Service | Port |
| --- | --- |
| App | 8080 (override with `SERVER_PORT`) |
| PostgreSQL | 5432 (override with `DB_PORT` for compose) |

Database `duke`, user `duke`, password `duke`. Override with `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## Schema

Flyway creates the schema on app startup from `src/main/resources/db/migration/V1__create_scores.sql` (table `score`).
Hibernate only validates it (`ddl-auto: validate`). A fresh database needs no manual step.

## Build, test, run

All commands run from `java-demo-copilot-cli-sonnet-5-5_2/`.

```
mvn verify                                   # build and run all tests
docker compose up -d                         # start the database
mvn spring-boot:run                          # run the app
java -jar target/java-demo-copilot-cli-sonnet-5-5_2-1.0.0.jar   # or run the packaged jar
```

Tests provision their own PostgreSQL 18 container with Testcontainers; Docker must be running, nothing else is needed.

## URLs

- Game: <http://localhost:8080/>
- Leaderboard: <http://localhost:8080/leaderboard.html>
- OpenAPI documentation (Swagger UI): <http://localhost:8080/swagger-ui.html>
- OpenAPI definition (JSON): <http://localhost:8080/v3/api-docs>

## API

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/games` | Start a game |
| POST | `/api/games/{gameId}/guesses` | Submit a guess (`{"guess": 1-10}`) |
| POST | `/api/games/{gameId}/score` | Post a won game's score (`{"name": "..."}`) |
| GET | `/api/leaderboard` | Top 10 wins, fewest guesses first, earliest first on ties |

Games live in memory (up to 10,000 recent ones) and are lost on restart; leaderboard entries are stored in PostgreSQL.

## Clear the leaderboard

The app cannot delete entries. Run from `java-demo-copilot-cli-sonnet-5-5_2/`:

```
docker compose exec postgres psql -U duke -d duke -c "TRUNCATE TABLE score RESTART IDENTITY"
```
