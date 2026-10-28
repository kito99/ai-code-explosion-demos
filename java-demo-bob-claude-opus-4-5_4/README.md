# java-demo-bob-claude-opus-4-5_4

**Guess the Number with Duke** — a server-side Java web game.

| Field | Value |
|---|---|
| Date of generation | 2026-10-28 |
| Agent harness | Bob |
| Model | Claude claude-opus-4-5 |

---

## Lines of Code

### Application code

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
github.com/AlDanial/cloc v 2.06  T=0.01 s (711.7 files/s, 32816.8 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             6             53             17            218
Maven                            1             13              8             80
Properties                       1              4              4             10
SQL                              1              1              0              7
-------------------------------------------------------------------------------
SUM:                             9             71             29            315
-------------------------------------------------------------------------------
```

### Test code

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
github.com/AlDanial/cloc v 2.06  T=0.01 s (153.5 files/s, 29095.7 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             2             61             30            288
-------------------------------------------------------------------------------
SUM:                             2             61             30            288
-------------------------------------------------------------------------------
```

---

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker (for the database and test Testcontainers)

---

## Supporting Services

### Start the database

Run from the `java-demo-bob-claude-opus-4-5_4/` directory:

```bash
docker compose up -d
```

### Stop the database

```bash
docker compose down
```

To also delete the stored data:

```bash
docker compose down -v
```

### Ports

| Service | Port |
|---|---|
| App | 8080 |
| PostgreSQL | 5432 |

### Database connection defaults

| Setting | Value |
|---|---|
| Database name | `guessdb` |
| User | `guessuser` |
| Password | `guesspass` |

---

## Schema Creation

Flyway runs automatically on startup. It reads migrations from
`src/main/resources/db/migration/` and applies any that have not yet been
applied. A fresh database ends up with the right tables with no manual step.

---

## Build

Run from the `java-demo-bob-claude-opus-4-5_4/` directory:

```bash
mvn package
```

The packaged JAR is written to `target/java-demo-bob-claude-opus-4-5_4.jar`.

---

## Test

Tests provision their own PostgreSQL container via Testcontainers. No database
needs to be running by hand.

Run from the `java-demo-bob-claude-opus-4-5_4/` directory:

```bash
mvn test
```

---

## Run

Start the database first (see above), then run from the
`java-demo-bob-claude-opus-4-5_4/` directory:

```bash
mvn spring-boot:run
```

Or, after building:

```bash
java -jar target/java-demo-bob-claude-opus-4-5_4.jar
```

---

## URLs

| Page | URL |
|---|---|
| Game | http://localhost:8080/ |
| Leaderboard | http://localhost:8080/leaderboard |

---

## Clear the Leaderboard

The app has no delete UI. To wipe all entries, connect to the database and
truncate the table:

```bash
docker exec -it java-demo-bob-claude-opus-4-5_4-db \
  psql -U guessuser -d guessdb -c "TRUNCATE TABLE leaderboard RESTART IDENTITY;"
```
