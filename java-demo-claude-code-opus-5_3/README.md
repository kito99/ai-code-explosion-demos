# java-demo-claude-code-opus-5_3

**Guess the Number with Duke** — a server-side Java web app built against [BRD.md](BRD.md).

## Generation metadata

| Field | Value |
|---|---|
| Date | 2026-10-28 |
| Agent harness | Claude Code (Bob) |
| Model | claude-opus-4-5 |

## Lines of code

### Application code

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
github.com/AlDanial/cloc v 2.06  T=0.02 s (830.7 files/s, 30261.9 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            11             66             69            258
Maven                            1              8              4             91
Properties                       1              2              0              6
SQL                              1              0              0              6
-------------------------------------------------------------------------------
SUM:                            14             76             73            361
-------------------------------------------------------------------------------
```

### Test code

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
github.com/AlDanial/cloc v 2.06  T=0.01 s (296.7 files/s, 27670.7 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             4             62             22            289
-------------------------------------------------------------------------------
SUM:                             4             62             22            289
-------------------------------------------------------------------------------
```

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for the database and for Testcontainers in tests)
- `cloc` (only needed to reproduce the line counts above)

## Supporting services

The app uses PostgreSQL 18 via Docker Compose.

```bash
# from java-demo-claude-code-opus-5_3/
docker compose up -d      # start the database
docker compose down       # stop and remove the container (data volume persists)
docker compose down -v    # stop and remove the container AND the data volume
```

The database container is named `java-demo-claude-code-opus-5_3-db`.

## Ports

| Service | Port |
|---|---|
| App | 8080 |
| PostgreSQL | 5432 |

## Database defaults

| Setting | Value |
|---|---|
| Database name | `guessdb` |
| User | `guessuser` |
| Password | `guesspass` |

These are the values the app defaults to in `src/main/resources/application.properties` and are also set in `compose.yaml`, so a fresh clone runs with no extra configuration.

## Schema creation

Flyway runs automatically at startup and applies the migrations in `src/main/resources/db/migration/`. A fresh database will have the correct schema after the first run with no manual step required.

## Build

All commands run from the `java-demo-claude-code-opus-5_3/` directory.

```bash
mvn package
```

The produced JAR is `target/java-demo-claude-code-opus-5_3.jar`.

## Test

Tests provision their own PostgreSQL instance via Testcontainers — no external database is required.

```bash
mvn test
```

## Run

Start the database first, then run the app.

```bash
# from java-demo-claude-code-opus-5_3/
docker compose up -d
mvn spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar target/java-demo-claude-code-opus-5_3.jar
```

The app starts on <http://localhost:8080>.

## URLs

| Page | URL |
|---|---|
| Game | <http://localhost:8080/> |
| Leaderboard | <http://localhost:8080/leaderboard> |

## Clearing the leaderboard

The app has no built-in delete feature. To clear all entries between runs:

```bash
# from java-demo-claude-code-opus-5_3/
docker compose exec db psql -U guessuser guessdb -c "DELETE FROM leaderboard_entry;"
```

Or stop the database, remove the volume, and restart:

```bash
docker compose down -v
docker compose up -d
```
