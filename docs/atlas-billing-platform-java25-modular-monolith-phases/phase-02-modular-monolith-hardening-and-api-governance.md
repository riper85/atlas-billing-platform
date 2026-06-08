# Phase 2 — Modular Monolith Hardening and API Governance

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 02 of 18

## Business Goal

The product grows. Different areas now need independent ownership:

- customer management
- catalog
- subscriptions
- invoicing
- payments
- ledger
- notifications

## Technical Goal

Harden the modular monolith so it does not become a big ball of mud while keeping one deployable application.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Modular monolith | Teams need boundaries before services exist |
| Package-private implementation | Internals should stay hidden |
| Public module APIs | Modules should collaborate through explicit contracts |
| Dependency direction rules | Architecture must prevent random coupling |
| ArchUnit | Boundary rules must be executable |
| API versioning | Clients will depend on endpoint behavior |
| Backward-compatible API changes | API evolution must not break consumers |
| API deprecation strategy | Old APIs need a planned retirement path |
| ETag | Clients need efficient reads and cache validation |
| `If-Match` | Updates need optimistic concurrency at the API boundary |
| Consumer-driven API evolution | Client needs influence API compatibility |
| ADR lifecycle | Decisions need ownership and review, not just files |
| Module ownership | Boundaries need responsible maintainers |

## Module Layout

```text
apps/billing-app/src/main/java/com/atlas/billing/
  customer/
    api/
    internal/
  catalog/
    api/
    internal/
  subscription/
    api/
    internal/
  invoice/
    api/
    internal/
  payment/
    api/
    internal/
  ledger/
    api/
    internal/
  notification/
    api/
    internal/
```

## Practical Implementation Tasks

- Move code into capability-based packages.
- Hide repositories and entities inside `internal` packages.
- Expose one public API per module.
- Add ArchUnit rules that prevent direct repository access across modules.
- Add an API versioning convention:

```text
/api/v1/...
/api/v2/...
```

- Add response headers for deprecation:

```http
Deprecation: true
Sunset: Wed, 31 Dec 2026 23:59:59 GMT
```

- Add ETag support for customer, plan, and subscription read endpoints.
- Add `If-Match` checks for update commands.
- Add optimistic concurrency errors as Problem Details.
- Add ADR status values:
  - Proposed
  - Accepted
  - Superseded
  - Deprecated

## Architectural Decision

### Decision

Keep one deployable app, but enforce internal module boundaries and API governance.

### Why

The system needs architectural discipline before operational distribution.

### Alternatives Considered

| Alternative | Why Not Yet |
|---|---|
| Extract microservices | Adds network, deployment, data consistency, and observability costs too early |
| Keep technical layers only | `controller/service/repository` packages hide domain ownership |
| No API governance | Versioning pain appears later when clients exist |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Strong boundaries without distributed systems | More package discipline |
| Easier future extraction | More explicit APIs required |
| Safer API evolution | More tests and documentation |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| ArchUnit tests | Module rules are enforced |
| API compatibility tests | v1 behavior does not break accidentally |
| ETag tests | Conditional reads and updates work |
| Contract smoke tests | API shape remains stable |

Acceptance criteria:

- Payment code cannot directly access invoice repositories.
- External API DTOs do not expose JPA entities.
- Conditional update with stale `If-Match` fails safely.
- ADRs exist for module boundaries and API versioning.

## Staff Engineer Lens

- A module boundary is a social boundary before it becomes a deployment boundary.
- Versioning is not only a URL problem; it is a compatibility promise.
- Good architecture prevents accidental shortcuts.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Spring Modulith verification** — Verify application modules, allowed dependencies, published events, and module documentation as part of `mvn verify`.
- **Module dependency matrix** — Maintain a small table showing which module may depend on which public API and why.
- **Shared kernel discipline** — Allow only stable cross-cutting concepts such as `Money`, `TenantId`, `CorrelationId`, and `ProblemDetails` into shared code.
- **Internal event boundaries** — Use module events for cross-module communication when direct synchronous calls would create coupling.
- **Public API maturity levels** — Mark module APIs as experimental, stable, deprecated, or internal-only to avoid accidental long-term coupling.

---
