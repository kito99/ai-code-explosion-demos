# Guess the Number with Duke — Jakarta EE Demo

A web-based guessing game backed by Open Liberty, Jakarta EE 11, and
MicroProfile 7.1. Duke (the Java mascot) thinks of a number between 1 and 10;
the player has 5 guesses to find it. Winning scores are posted to a persistent
leaderboard.

---

## Generation metadata

| Field | Value |
|---|---|
| Date of generation | 2026-10-27 |
| Agent harness | Bob (IBM Bob) |
| Model | claude-opus-4-5 |

### Lines of code

Commands run from `jakarta-ee-demo-bob-claude-opus-4-5-1/`.

**Application code** (Java + Maven + XML + config; excludes HTML, CSS, JS, SVG):

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
github.com/AlDanial/cloc v 2.06  T=0.02 s (774.6 files/s, 36732.1 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            14             88             72            452
Maven                            1              8             18            161
XML                              3             11              3             72
Properties                       1              2              9              5
-------------------------------------------------------------------------------
SUM:                            19            109            102            690
-------------------------------------------------------------------------------
```

**Test code**:

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
github.com/AlDanial/cloc v 2.06  T=0.02 s (256.2 files/s, 31708.1 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             4             69             27            399
-------------------------------------------------------------------------------
SUM:                             4             69             27            399
-------------------------------------------------------------------------------
```

---

## Architecture overview

| Layer | Technology |
|---|---|
| HTTP runtime | Open Liberty 24.x |
| REST API | Jakarta RESTful Web Services (JAX-RS) 4.0 |
| CDI | Jakarta CDI 4.1 |
| Persistence | Jakarta Persistence (JPA) 3.2 — EclipseLink |
| JSON binding | Jakarta JSON-B 3.0 |
| Observability | MicroProfile Telemetry 2.1 (OpenTelemetry) |
| API docs | MicroProfile OpenAPI 4.0 |
| Database | PostgreSQL 18 |
| Front-end | Vanilla HTML / CSS / JavaScript (served as static files) |

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for the database and for Testcontainers in tests)
- `cloc` (only to reproduce the line counts above)

---

## Supporting services

### Start

```bash
# from jakarta-ee-demo-bob-claude-opus-4-5-1/
docker compose up -d
```

### Stop

```bash
# from jakarta-ee-demo-bob-claude-opus-4-5-1/
docker compose down
```

### Ports

| Service | Port |
|---|---|
| Application (HTTP) | 9080 |
| Application (HTTPS) | 9443 |
| PostgreSQL | 5432 |

---

## Database

| Setting | Value |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database name | `guessdb` |
| Username | `guessuser` |
| Password | `guesspass` |

### Schema creation

The schema is generated automatically by JPA (EclipseLink) on first start using:

```xml
<property name="jakarta.persistence.schema-generation.database.action" value="create"/>
```

No manual SQL step is required on a fresh database.

### Clear the leaderboard between runs

```sql
psql -h localhost -U guessuser -d guessdb -c "DELETE FROM leaderboard_entry;"
```

Or via Docker:

```bash
docker compose exec db psql -U guessuser -d guessdb -c "DELETE FROM leaderboard_entry;"
```

---

## Build

```bash
# from jakarta-ee-demo-bob-claude-opus-4-5-1/
mvn package
```

This creates, configures, and packages the Liberty server under
`target/liberty/wlp/`.

---

## Run

Start the database first (see above), then:

```bash
# from jakarta-ee-demo-bob-claude-opus-4-5-1/
mvn liberty:run
```

Or start Liberty in the background:

```bash
# from jakarta-ee-demo-bob-claude-opus-4-5-1/
mvn liberty:start
```

Stop:

```bash
# from jakarta-ee-demo-bob-claude-opus-4-5-1/
mvn liberty:stop
```

The game is served at <http://localhost:9080/>.

---

## Test

Unit tests run without any external dependencies:

```bash
# from jakarta-ee-demo-bob-claude-opus-4-5-1/
mvn test
```

Integration tests (`*IT`) start a PostgreSQL Testcontainer and the Liberty
server automatically — no database or server needs to be running beforehand:

```bash
# from jakarta-ee-demo-bob-claude-opus-4-5-1/
mvn verify
```

---

## URLs

| URL | Description |
|---|---|
| <http://localhost:9080/> | Game |
| <http://localhost:9080/leaderboard.html> | Leaderboard |
| <http://localhost:9080/openapi> | OpenAPI definition (JSON/YAML) |
| <http://localhost:9080/openapi/ui> | Interactive API documentation (Swagger UI) |

---

## Telemetry

Traces, metrics, and logs are exported to the **console** by default using the
OpenTelemetry `logging` exporter. Look for lines prefixed with `[otel]` or the
span/metric exporter output in the Liberty console log.

To point the app at a real OTLP collector, change these properties in
[`src/main/resources/META-INF/microprofile-config.properties`](src/main/resources/META-INF/microprofile-config.properties):

```properties
otel.exporter.otlp.endpoint=http://your-collector:4317
otel.traces.exporter=otlp
otel.metrics.exporter=otlp
otel.logs.exporter=otlp
```

No code change is required — MicroProfile Config picks up these values at
startup.
