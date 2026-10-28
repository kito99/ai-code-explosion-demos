# java-demo-copilot-cli-sonnet-5-5_5 — Guess the Number with Duke

Spring Boot (Java 21+) server with a plain HTML/JS front-end and PostgreSQL 18, built from `BRD.md`.

## Generation details

* **Date of generation:** 2026-10-28
* **Agent harness:** GitHub Copilot CLI (1.0.103)
* **Model:** Claude Sonnet 5.5 (`claude-sonnet-5.5`)

## Lines of code (cloc 2.06)

Run from `java-demo-copilot-cli-sonnet-5-5_5/`.

Application code (310 lines):

```
$ cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             7             35              3            232
Maven                            1              6              0             64
SQL                              1              1              0              8
Properties                       1              2              1              6
-------------------------------------------------------------------------------
SUM:                            10             44              4            310
-------------------------------------------------------------------------------
```

Test code (149 lines):

```
$ cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             2             18              1            149
-------------------------------------------------------------------------------
SUM:                             2             18              1            149
-------------------------------------------------------------------------------
```

## Supporting services

Run from `java-demo-copilot-cli-sonnet-5-5_5/`:

```
docker compose up -d      # start PostgreSQL 18 (container java-demo-copilot-cli-sonnet-5-5_5-db)
docker compose down       # stop it (data kept in the db-data volume)
docker compose down -v    # stop and delete the data
```

## Ports and defaults

| Item | Value |
| --- | --- |
| App | `8085` |
| PostgreSQL (host) | `5445` (container `5432`) |
| Database / user / password | `duke` / `duke` / `duke` |

Override with `DB_URL`, `DB_USER`, `DB_PASSWORD` environment variables.

## Schema creation

`src/main/resources/schema.sql` is run by Spring on every startup
(`spring.sql.init.mode=always`). It uses `CREATE TABLE IF NOT EXISTS`, so a fresh
database gets the `leaderboard_entry` table automatically and existing data is untouched.

## Build, test, run

All from `java-demo-copilot-cli-sonnet-5-5_5/`:

```
mvn test                                                # tests start their own PostgreSQL via Testcontainers (Docker required)
mvn package                                             # builds target/java-demo-copilot-cli-sonnet-5-5_5.jar
docker compose up -d --wait                             # database
java -jar target/java-demo-copilot-cli-sonnet-5-5_5.jar # run the app (or: mvn spring-boot:run)
```

## URLs

* Game: <http://localhost:8085/>
* Leaderboard: <http://localhost:8085/leaderboard.html>

## Clearing the leaderboard

From `java-demo-copilot-cli-sonnet-5-5_5/`:

```
docker compose exec db psql -U duke -d duke -c "TRUNCATE leaderboard_entry RESTART IDENTITY"
```

## Notes

* The secret number and guess count live on the server (HTTP session); the browser never sees the secret until the game is lost.
* Duke images (`Thinking.svg`, `Wave.svg`) are from <https://github.com/openjdk/duke>.
