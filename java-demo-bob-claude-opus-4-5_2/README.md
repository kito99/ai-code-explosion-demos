# java-demo-bob-claude-opus-4-5_2

## Generation metadata

| Field | Value |
|---|---|
| Date of generation | 2026-10-28 |
| Agent harness | Bob |
| Model | Claude claude-opus-4-5 |

## Lines of code

### Application code

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
github.com/AlDanial/cloc v 2.06  T=0.02 s (1138.4 files/s, 38798.2 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            23            107             88            486
Maven                            1             13              9            109
YAML                             1              4              0             36
-------------------------------------------------------------------------------
SUM:                            25            124             97            631
-------------------------------------------------------------------------------
```

### Test code

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
github.com/AlDanial/cloc v 2.06  T=0.02 s (328.7 files/s, 37470.3 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             4            105             24            428
YAML                             1              1              0             12
-------------------------------------------------------------------------------
SUM:                             5            106             24            440
-------------------------------------------------------------------------------
```

## Overview

Spring Boot 3.5 / Java 25 implementation of the "Guess the Number with Duke" game, generated from `BRD.md`.

**Stack:** Spring Boot · Spring Data JPA · PostgreSQL 18 · Micrometer / OpenTelemetry · springdoc-openapi

## Running the supporting services

```bash
# from java-demo-bob-claude-opus-4-5_2/
docker compose up -d
```

Stop with:

```bash
docker compose down
```

To wipe the database volume as well (clears all leaderboard entries):

```bash
docker compose down -v
```

## Ports

| Service | Port |
|---|---|
| App (HTTP) | 8080 |
| PostgreSQL | 5432 |

## Database defaults

| Setting | Value |
|---|---|
| Database name | `dukedb` |
| Username | `duke` |
| Password | `duke` |

## Schema creation

Hibernate's `ddl-auto: update` creates the `leaderboard_entry` table automatically on first startup. No manual SQL step is needed.

## Building

```bash
# from java-demo-bob-claude-opus-4-5_2/
mvn package -DskipTests
```

## Running tests

Tests provision their own PostgreSQL container via Testcontainers. Docker (or compatible) must be running:

```bash
# from java-demo-bob-claude-opus-4-5_2/
mvn test
```

## Starting the app

Start the database first, then:

```bash
# from java-demo-bob-claude-opus-4-5_2/
mvn spring-boot:run
```

Or run the fat JAR:

```bash
java --enable-preview -jar target/java-demo-bob-claude-opus-4-5_2.jar
```

## URLs

| Resource | URL |
|---|---|
| Game | http://localhost:8080/ |
| Leaderboard | http://localhost:8080/leaderboard.html |
| OpenAPI JSON | http://localhost:8080/api-docs |
| Swagger UI | http://localhost:8080/swagger-ui.html |

## Telemetry

By default traces are exported to the console log via `opentelemetry-exporter-logging` and metrics via Micrometer's OTLP registry. No external collector is needed.

To point at a real OTLP collector, update `src/main/resources/application.yaml`:

```yaml
management:
  otlp:
    tracing:
      endpoint: http://your-collector:4318/v1/traces
    metrics:
      export:
        url: http://your-collector:4318/v1/metrics
```

The service name is `java-demo-bob-claude-opus-4-5_2` (set via `spring.application.name` from the artifact id).

## Clearing the leaderboard

The app has no delete endpoint. To clear all entries between runs:

```bash
# stop the app, then:
docker compose down -v   # removes the pgdata volume
docker compose up -d     # fresh database
```

Or connect to PostgreSQL directly:

```bash
docker compose exec db psql -U duke -d dukedb -c "TRUNCATE leaderboard_entry RESTART IDENTITY;"
```
