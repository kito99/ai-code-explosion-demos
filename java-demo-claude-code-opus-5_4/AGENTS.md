# AGENTS.md

This file provides guidance for coding agents to work with this monorepo. Each repo may have its own AGENTS.md with more specific guidelines.

# Project Instructions

- Use `README.md` for developer / build instructions.

# General

- Always use enums rather than raw strings 
- Documentation for classes should describe only behavior
- Aim for a class file length of around 500 lines; if it’s longer, split it into multiple classes/files.

# Testing Guidelines

- Write unit tests for all new functionality
- Write or update e2e test for new features / flows
- Mock external dependencies when appropriate
- Ensure tests are deterministic and isolated

# Exception Handling

- Exceptions or Errors should only be caught without rethrowing (i.e. swallowed) when the calling code can properly recover without side-effects
- Favor unchecked exceptions
- Use custom exceptions or errors when the type can be a helpful description of the error (e.g. PersistenceException or SyntaxException)
- Use base exceptions when it makes sense semantically, or you need to reuse core logic

# Java

- Use modern Java (25+) syntax and features unless the repository is using an older version
- Use static imports when possible
- Use the fully qualified package name when referencing a class only when there is another class with the same name. 
- Use var unless the lack of the type is ambiguous.
- Use final whenever possible.
- Use _ for variables which aren’t used.
- Use atomic variables or other synchronization primitives rather than the synchronized keyword when possible.
- Use longer, easy to read variable names that spell out full words or common abbreviations.
- Ceate custom exception types if the defaults (like NullPointerException or IllegalArgumentException) are not relevant. Use RuntimeException when for new exception types.
- For building strings, use formatting features like String.format().
- For long strings, use text blocks.
- If a return value could possibly be null, use Optional.
- If a Collection could possibly be null, always use an empty list and uses Objects.requireNonNull() to avoid nulls.
- Favor using virtual threads all the way down the call stack unless you’re modifying existing reactive code.
- Use Markdown for Javadoc comment (JDK 23+)
- Prefer using streams and lambdas for iterations and transformations.

# Development Instructions

- When writing code, respect DRY, KISS, and Law of Demeter; keep Cyclomatic Complexity below 15; do not use magic numbers.
- After completing all code changes, review if project `README.md` needs to abe updated based on your changes, and update if necessary.


# Writing Style

- Write AI responses, commit messages, PR descriptions, and code comments terse and direct. No preamble, no padding, no recap of what you just did. Headers and tables only when the content genuinely benefits from structure — otherwise prose. Mirror the user's register (casual ↔ formal) rather than defaulting to either extreme.
- Be forward-looking, not postmortem. When documenting findings, decisions, or procedures, write them as a reference for the next reader — not a narrative of what just happened. ("X works like Y", not "we tried Y and then found X").

# General Principles

- Respect the Law of Demeter: maintain separation of concerns, don't reach through objects, don't overengineer.
- Respect DRY: before writing new logic, scan the codebase for same/similar patterns and refactor/reuse.
- Respect KISS: keep cyclomatic complexity of each method below 15, else extract into smaller, reusable methods.
- Prefer declarative over imperative style when it improves readability, unless it impacts performance.
- No magic numbers: extract them into named constants with clear intent. String literals are fine unless duplicated more than twice.
- No redundant comments: don't comment what the code already says (method names, variable names, obvious flow). Only comment *why* something is done when it's non-obvious, or to clarify tricky logic. Never duplicate javadoc in an inline comment. Section markers (e.g. `// --- Lifecycle ---`) are fine in large files.
- No bug-postmortem comments: incident-specific detail (run numbers, "after we hit X", smoking-gun datapoints, before/after measurements) belongs in the commit message or PR description, not in source comments. Source comments should state the forward-looking *why* (the invariant, constraint, or contract that justifies the code) — not narrate how a past bug was diagnosed. Postmortem detail rots as the codebase evolves; commit history doesn't.

# Assessments, Debugging, and Bug Fixes

- When fixing a bug, first write a failing test.
- State your diagnosis and which file/method you'll change BEFORE writing code. Always wait for confirmation. This also applies to non-trivial new features: describe the approach before implementing.
- Do not over-apply fixes. Stay within the requested scope.

# Self-Review

- When working on a task, after completing all code changes, spawn a "Code Reviewer" agent at high effort using a different model and perform a self-review of the full diff before presenting the result. Check for:
  - Violations of the General Principles above
  - Missing edge cases or error handling at system boundaries
  - Naming/style/formatting consistency with surrounding code
  - Security: injection risks, sensitive data exposure, unsanitized input at system boundaries
  - Unnecessary changes or scope creep
  - Ripple effects: when changing a method signature, callback contract, or API, grep for ALL callers/consumers (including sandbox tests, unit tests, and integration tests) and update them
  - Unit/integration test coverage for new/changed logic (only when project already has test setup)
  - Anything a human expert would reasonably flag
- Do NOT flag intentional design choices as bugs. If unsure, list separately as "needs clarification". Add a clarifying inline comment to prevent future false flags.

# Version Control

- NEVER commit or push anything without explicit confirmation.
- ALWAYS prefix a commit message with the issue # (if applicable).

