---
name: context7-mcp
description: This skill should be used when the user asks about libraries, frameworks, SDKs, APIs, CLI tools, or cloud services, or needs code examples. Activates for setup and configuration questions, version migration, library-specific debugging, and mentions of specific frameworks like Jakarta EE, Open Liberty, MicroProfile, Spring Boot, React, or Next.js.
---

Fetch current documentation through Context7 instead of answering libraries and
frameworks from training data. Use it even when the answer seems obvious, since
training data lags behind releases.

## When to use

- Setup or configuration questions ("How do I configure MicroProfile Telemetry?")
- Code involving a library ("Write a JPA entity for the leaderboard")
- API references ("What are the valid `otel.traces.exporter` values?")
- Version migration, library-specific debugging, or CLI tool usage
- Any mention of a specific framework, SDK, or cloud service

## When not to use

Skip Context7 for refactoring, writing scripts from scratch, debugging this
project's own logic, code review, and general programming concepts. Those do not
depend on external documentation.

## How to fetch documentation

1. **Resolve the library.** Call `resolve-library-id` with `libraryName` set to
   the library name and `query` set to the user's full question, which improves
   relevance ranking. Skip this step only when the user supplied an exact ID in
   `/org/project` form.
2. **Pick the best match.** Prefer an exact name match, then description
   relevance, snippet count, source reputation, and benchmark score. Use a
   version-specific ID when the user named a version. If nothing looks right,
   try an alternate spelling, such as "next.js" rather than "nextjs".
3. **Query the docs.** Call `query-docs` with the chosen `libraryId` and the
   user's full question, scoped to one concept. Make separate calls for separate
   concepts rather than combining them into one broad query.
4. **Answer from what came back**, citing the version when it matters.

## Notes for this repository

The demos here are generated from `BRD.md` and the prompt files, so the stack
named in a prompt is the stack to look up. Prefer Context7 over web search for
Open Liberty, Jakarta EE, MicroProfile, and PostgreSQL questions, and check the
docs before hand-rolling anything the specification already covers.
