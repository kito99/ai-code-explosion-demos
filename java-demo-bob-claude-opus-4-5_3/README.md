# java-demo-bob-claude-opus-4-5_3

**Guess the Number with Duke** — a server-side Java guessing game with a leaderboard.

## Generation metadata

| Field | Value |
|---|---|
| Date of generation | 2026-10-28 |
| Agent harness | Bob (IBM Bob) |
| Model | Claude claude-opus-4-5 |

## Lines of code

Commands run from `java-demo-bob-claude-opus-4-5_3/`:

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

### Application code

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            18             91             83            440
Maven                            1              9              4             94
Properties                       1              5              5             10
SQL                              1              1              0              7
-------------------------------------------------------------------------------
SUM:                            21            106             92            551
-------------------------------------------------------------------------------
```

### Test code

```
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             3             54             21            310
Properties                       1              0              0              4
-------------------------------------------------------------------------------
SUM:                             4             54             21            314
-------------------------------------------------------------------------------
```

## Prerequisites

- Java 25+
- Maven 3.9+
- Docker (for the database and Testcontainers)
- `cloc` (for reproducing line counts)

## Supporting services

The app requires a PostgreSQL 18 database. A `compose.yaml` is included.

```bash
# From java-demo-bob-claude-opus-4-5_3/
docker compose up -d      # start the database
docker compose down       # stop and remove the container (data is preserved in the volume)
docker compose down -v    # stop and delete data as well
```

**Port:** PostgreSQL listens on `localhost:5432`.

**Database name:** `guessgame`  
**Database user:** `guessgame`  
**Database password:** `guessgame`

## Schema creation

Flyway manages the schema. Migrations live in `src/main/resources/db/migration/`.
On first startup the app automatically creates the `leaderboard_entries` table.
No manual SQL step is required.

## Build

```bash
# From java-demo-bob-claude-opus-4-5_3/
mvn package
```

The produced JAR is `target/java-demo-bob-claude-opus-4-5_3.jar`.

## Test

Tests spin up their own PostgreSQL container via Testcontainers — no running
database is required.

```bash
# From java-demo-bob-claude-opus-4-5_3/
mvn test
```

## Run

```bash
# 1. Start the database first
# From java-demo-bob-claude-opus-4-5_3/
docker compose up -d

# 2. Run the app
# From java-demo-bob-claude-opus-4-5_3/
mvn spring-boot:run
# or, after a package build:
java -jar target/java-demo-bob-claude-opus-4-5_3.jar
```

**App port:** `8080`

## URLs

| Resource | URL |
|---|---|
| Game | http://localhost:8080/ |
| Leaderboard screen | http://localhost:8080/leaderboard.html |
| OpenAPI definition (JSON) | http://localhost:8080/api-docs |
| Swagger UI (interactive docs) | http://localhost:8080/swagger-ui.html |

## Clearing the leaderboard

The app has no delete endpoint. To reset between runs, truncate the table:

```bash
docker exec -it java-demo-bob-claude-opus-4-5_3-db \
  psql -U guessgame -d guessgame -c "TRUNCATE leaderboard_entries RESTART IDENTITY;"
```
