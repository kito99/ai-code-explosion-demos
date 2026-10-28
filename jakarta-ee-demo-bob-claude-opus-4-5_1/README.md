# jakarta-ee-demo-bob-claude-opus-4-5_1

Guess the Number with Duke — Jakarta EE 11 / Open Liberty demo.

## Generation details

| Field | Value |
|---|---|
| Date | 2026-10-28 |
| Agent harness | Bob (IBM Bob) |
| Model | Claude claude-opus-4-5 |
| Open Liberty version | 26.0.0.10 |
| Jakarta EE version | 11.0 |

## Lines of code

Commands run from the `jakarta-ee-demo-bob-claude-opus-4-5_1/` directory.

**Application code** (Java + build + config, no HTML/CSS/JS/SVG):

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/main pom.xml
```

```
github.com/AlDanial/cloc v 2.06  T=0.02 s (682.0 files/s, 29967.2 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                            11             65             66            282
Maven                            1             12              8            199
XML                              3             10              2             43
Properties                       1              2              9              5
-------------------------------------------------------------------------------
SUM:                            16             89             85            529
-------------------------------------------------------------------------------
```

**Test code**:

```
cloc --exclude-lang=HTML,CSS,JavaScript,SVG,Markdown,Text src/test
```

```
github.com/AlDanial/cloc v 2.06  T=0.01 s (160.3 files/s, 20440.9 lines/s)
-------------------------------------------------------------------------------
Language                     files          blank        comment           code
-------------------------------------------------------------------------------
Java                             2             36             20            199
-------------------------------------------------------------------------------
SUM:                             2             36             20            199
-------------------------------------------------------------------------------
```

## Architecture

- **Backend:** Open Liberty 26.0.0.10 with Jakarta EE 11 (`jakartaee-11.0` feature)
- **Database:** Apache Derby embedded — no external database required; data lives under `target/liberty/wlp/usr/servers/defaultServer/data/leaderboard/`
- **Game sessions:** held in-memory (`ConcurrentHashMap`) — refreshing starts a new game
- **Leaderboard:** persisted to Derby via Jakarta Data + Jakarta Persistence

## Ports

| Protocol | Port |
|---|---|
| HTTP | 9080 |
| HTTPS | 9443 |

## Build, test, and run

All commands must be run from the `jakarta-ee-demo-bob-claude-opus-4-5_1/` directory.

**Build and run unit tests only:**

```bash
mvn test
```

**Build, run unit tests + integration tests (starts and stops Liberty automatically):**

```bash
mvn verify
```

**Run the app (starts Liberty in the foreground; Ctrl-C stops it):**

```bash
mvn liberty:run
```

Once running, open: <http://localhost:9080/>

## Leaderboard URL

<http://localhost:9080/> — use the "🏆 Leaderboard" button in the app, or navigate to the leaderboard directly via the in-app navigation.

## Clearing the leaderboard

The app has no delete API. To clear all entries between runs, stop the server and delete the Derby database directory:

```bash
rm -rf target/liberty/wlp/usr/servers/defaultServer/data/leaderboard
```

The next `mvn liberty:run` or `mvn verify` recreates the schema automatically.

## Other dependencies

None. Derby is bundled and copied into the Liberty shared-resources directory during the build. No running database or other external service is needed.
