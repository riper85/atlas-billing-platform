# Phase 16 — Quality, Testing, and Governance

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 16 of 18

## Business Goal

The platform now has multiple modules, services, events, APIs, migrations, and operational rules. Quality must not rely on memory.

## Technical Goal

Automate engineering standards, compatibility checks, architecture rules, and release confidence.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Test pyramid | Different tests provide different confidence/cost |
| Test slice strategy | Not every test should start the whole app |
| Contract test provider verification | Services need compatibility confidence |
| Mutation testing | Coverage should prove assertion quality |
| Property-based testing | Money and state rules benefit from generated cases |
| Approval testing | Complex reports can be compared safely |
| Golden master testing | Existing behavior can be protected during refactoring |
| Concurrency testing | Race conditions need explicit tests |
| Chaos testing light | Failure scenarios should be rehearsed locally |
| Fault injection | Dependencies should fail in tests |
| Flaky test detection | Unreliable tests destroy trust |
| Database migration tests | Schema evolution must be safe |
| Performance regression tests | Latency should not degrade silently |
| Static analysis | Quality issues should be caught automatically |
| Architecture fitness function | Architecture rules should run continuously |
| Technical debt register | Known trade-offs need visibility |
| Dependency update strategy | Dependencies must be maintained safely |
| Code ownership | Critical areas need responsible reviewers |
| PR checklist | Reviews need consistent expectations |
| Governance without bottlenecks | Standards should enable speed, not block everything |

## Practical Implementation Tasks

- Add GitHub Actions pipeline:

```text
format -> compile -> unit tests -> integration tests -> architecture tests -> security scan -> package
```

- Add JaCoCo thresholds by module.
- Add ArchUnit checks for dependency direction.
- Add Pact verification in CI.
- Add mutation testing with PIT for domain modules.
- Add migration tests against PostgreSQL Testcontainers.
- Add lightweight chaos tests:
  - payment provider timeout
  - Kafka unavailable
  - Redis unavailable
  - database connection exhaustion
- Add performance regression smoke test for payment authorization.
- Add flaky test quarantine policy.
- Add technical debt register:

```text
docs/technical-debt.md
```

- Add architecture fitness functions:
  - no domain dependency on Spring
  - no cross-module repository access
  - no controller returning JPA entities
  - no service accessing another service database
  - every Kafka consumer must have idempotency handling
- Add dependency update process.
- Add CODEOWNERS.
- Add PR checklist.

## Architectural Decision

### Decision

Automate governance in the build pipeline and keep human review focused on judgment.

### Why

Rules that can be checked automatically should not depend on reviewer memory.

### Alternatives Considered

| Alternative | Why Not Enough |
|---|---|
| Code review only | Humans miss repeated mechanical issues |
| Documentation only | Docs drift from code |
| Manual release checklist only | Does not scale |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Consistent quality | CI can become slow |
| Fewer repeated review comments | Rules need maintenance |
| Better architecture protection | False positives can frustrate developers |

## Testing and Acceptance Gate

| Check | Acceptance Criteria |
|---|---|
| CI pipeline | Runs consistently on pull requests |
| Fitness functions | Block architecture violations |
| Mutation testing | Critical domain tests catch real behavior changes |
| Contract tests | Provider and consumer stay compatible |
| Security scans | Known dependency and secret issues are visible |

## Staff Engineer Lens

- Governance should be lightweight, automated, and useful.
- CI is an engineering feedback system.
- Technical debt should be visible, owned, and periodically reviewed.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **CI time budget** — Classify checks as PR-fast, nightly, release, and manual so governance does not make feedback painfully slow.
- **Test data builders and fixtures** — Standardize realistic customers, plans, invoices, payments, ledger entries, and events.
- **Flaky-test quarantine with expiry** — Quarantined tests need owner, reason, expiry date, and issue link; otherwise they become ignored forever.
- **SBOM verification gate** — Generate and store SBOM artifacts and check them during release packaging.
- **Architecture fitness trend** — Track rule violations over time, not only current pass/fail state.

---
