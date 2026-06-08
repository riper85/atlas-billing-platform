# ADR-0001 — Use Maven Multi-Module Monorepo

## Status

Accepted

## Context

The platform starts as a modular monolith. The domain boundaries are not proven yet, so early independent deployment would create coordination cost before it creates business value.

## Decision

Use one Git repository with a Maven reactor and clear module conventions.

## Alternatives Considered

| Alternative | Why Not Yet |
|---|---|
| Many repositories | Too much release coordination before service boundaries are validated |
| Gradle | Good option, but Maven is common in enterprise Java and easier for this learning path |
| Single flat project | Dependency and boundary governance becomes weak as code grows |

## Consequences

| Positive | Negative |
|---|---|
| Fast refactoring across modules | Repository can become large |
| Centralized dependency governance | Requires discipline around module boundaries |
| Easier local onboarding | Initial build setup is more explicit |

## Review Date

2026-09-01
