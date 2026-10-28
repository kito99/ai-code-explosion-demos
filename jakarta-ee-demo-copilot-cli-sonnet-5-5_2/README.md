# Guess the Number with Duke (Jakarta EE 11 / MicroProfile 7.1)

A web game where Duke thinks of a number from 1 to 10 and you get 5 guesses.
Winners can post their score to a shared leaderboard. Built from `BRD.md`.

## Generation details

| | |
| --- | --- |
| Date of generation | 2026-10-28 |
| Agent harness | GitHub Copilot CLI (1.0.105) |
| Model | Claude Sonnet 5.5 (`claude-sonnet-5.5`) |

## Stack

- Open Liberty 26.0.0.10, features `jakartaee-11.0` and `microProfile-7.1`
- Jakarta REST (API), Jakarta Validation (guess and name rules), Jakarta Data +
  Jakarta Persistence (leaderboard repository), Jakarta CDI, JSON-B
- MicroProfile Health (`/health/ready` checks the database) and OpenAPI
- PostgreSQL 18.6 (official Docker image, pinned)
- Plain HTML/CSS/JS front end; Duke images from <https://github.com/openjdk/duke>

## Ports and defaults

| Item | Value |
| --- | --- |
| App (HTTP) | 9080 (override with `HTTP_PORT`) |
| Database (host port) | 5435 (container port 5432) |
| Database name / user / password | `duke` / `duke` / `duke` |
| JDBC URL default | `jdbc:postgresql://localhost:5435/duke` (override with `DB_URL`, `DB_USER`, `DB_PASSWORD`) |

The host port is 5435 so the database does not collide with a PostgreSQL
already listening on 5432.

## Supporting services

Run from `jakarta-ee-demo-copilot-cli-sonnet-5-5_2/`:

```
docker compose up -d      # start PostgreSQL
docker compose down       # stop it (data is kept in the duke-data volume)
docker compose down -v    # stop it and delete the data
```

## Build, test, run

All commands run from `jakarta-ee-demo-copilot-cli-sonnet-5-5_2/`.

```
mvn verify                # build the WAR, run unit tests and integration tests
mvn liberty:run           # run the app (needs `docker compose up -d` first)
```

Then open <http://localhost:9080/>. Stop `liberty:run` with Ctrl+C.

The WAR is `target/jakarta-ee-demo-copilot-cli-sonnet-5-5_2.war`.

### Tests

- `GameTest` (surefire, `mvn test`): game rules, validation, and number draws.
- `GameApiIT` (failsafe, `mvn verify`): starts its own PostgreSQL 18.6 with
  Testcontainers and a real Open Liberty server on a free port, then exercises
  the REST API end to end, including leaderboard ordering and survival across a
  server restart. Docker must be running; nothing else needs starting by hand.
  The server log is in `target/liberty/liberty-it.log`.

## Schema creation

The leaderboard table (`leaderboard_entry`) is created automatically: the
`databaseStore` in `src/main/liberty/config/server.xml` sets `createTables="true"`,
so Jakarta Data creates the table on first use if it is missing. No manual SQL
is needed on a fresh database.

## URLs

- Game: <http://localhost:9080/>
- Leaderboard screen: <http://localhost:9080/leaderboard.html>
- REST API: `POST /api/games`, `POST /api/games/{id}/guesses`,
  `POST /api/games/{id}/score`, `GET /api/leaderboard`
- OpenAPI: <http://localhost:9080/openapi>

## Clearing the leaderboard

The app cannot delete entries. Either wipe the table (run from
`jakarta-ee-demo-copilot-cli-sonnet-5-5_2/`):

```
docker compose exec db psql -U duke -d duke -c "TRUNCATE leaderboard_entry RESTART IDENTITY"
```

or remove the whole database volume with `docker compose down -v`.

## Lines of code (cloc 2.06)

Front-end code (HTML, CSS, JavaScript) and SVG assets are left out. Run from
`jakarta-ee-demo-copilot-cli-sonnet-5-5_2/`.

Application code:

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            10             92              5            376
Maven                            1              5              1            151
XML                              1              7              2             21
-------------------------------------------------------------------------------
SUM:                            12            104              8            548
-------------------------------------------------------------------------------
```

Test code:

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             2             43              6            321
-------------------------------------------------------------------------------
SUM:                             2             43              6            321
-------------------------------------------------------------------------------
```
