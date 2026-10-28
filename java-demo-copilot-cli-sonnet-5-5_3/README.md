# java-demo-copilot-cli-sonnet-5-5_3

"Guess the Number with Duke": guess Duke's secret number (1–10) in 5 tries and post wins to a shared leaderboard. Built from `BRD.md` per `demo-prompt-common.md` (prompt: `java-prompt.txt`).

## Generation

| | |
| --- | --- |
| Date | 2026-10-28 |
| Agent harness | GitHub Copilot CLI 1.0.103 |
| Model | Claude Sonnet 5.5 (`claude-sonnet-5.5`) |

## Stack

Java 25, Javalin 6 (Jetty), HikariCP, PostgreSQL 18, plain HTML/CSS/JavaScript front end. Duke images are from [openjdk/duke](https://github.com/openjdk/duke) (`Duke.Hips.3D`, `Duke.Wave1.3D`, `Duke.Wave2.3D`), downscaled.

The secret number lives on the server; the page only receives feedback. Games are held in memory (a refresh starts a new game); only leaderboard entries are stored in PostgreSQL.

## Supporting services

Run from `java-demo-copilot-cli-sonnet-5-5_3/`:

```
docker compose up -d      # start PostgreSQL 18 (returns once started; check with: docker compose ps)
docker compose down       # stop it, keeping leaderboard data
docker compose down -v    # stop it and delete leaderboard data
```

If host port 5432 is taken, start the database on another port with `DB_PORT=55432 docker compose up -d` and run the app with `DATABASE_URL=jdbc:postgresql://localhost:55432/duke`.

## Ports and defaults

| | |
| --- | --- |
| App | `8080` (override with `PORT`) |
| Database | `5432` (host and container) |
| Database name / user / password | `duke` / `duke` / `duke` |
| Override variables | `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` |

## Schema

`SchemaInitializer` runs `src/main/resources/schema.sql` (`CREATE TABLE IF NOT EXISTS`) on every app start, so a fresh database needs no manual step. Integration tests exercise the same path.

## Build, test, run

All from `java-demo-copilot-cli-sonnet-5-5_3/`:

```
mvn verify                                                  # build, run all tests, package the jar
mvn test                                                    # tests only
docker compose up -d && java -jar target/java-demo-copilot-cli-sonnet-5-5_3.jar
```

Tests start their own PostgreSQL 18 container with Testcontainers; Docker must be running, nothing else is needed.

- Game: http://localhost:8080/
- Leaderboard: http://localhost:8080/leaderboard

## Clearing the leaderboard

From `java-demo-copilot-cli-sonnet-5-5_3/`:

```
docker compose exec database psql -U duke -d duke -c "TRUNCATE leaderboard_entry RESTART IDENTITY"
```

or `docker compose down -v` to drop the whole volume.

## Lines of code

Run from `java-demo-copilot-cli-sonnet-5-5_3/` (front-end files, SVG, Markdown excluded):

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            21            104             29            534
Maven                            1              5              0            120
SQL                              1              1              0              8
-------------------------------------------------------------------------------
SUM:                            23            110             29            662
-------------------------------------------------------------------------------
```

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             6             75              1            407
-------------------------------------------------------------------------------
SUM:                             6             75              1            407
-------------------------------------------------------------------------------
```

Application code: 662 lines. Test code: 407 lines.
