# Phase 7 — Platform Libraries and Spring Internals

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 07 of 18

## Business Goal

Repeated infrastructure logic appears across modules and future services:

- error handling
- logging
- validation
- security context
- JSON configuration
- idempotency
- caching
- tracing

## Technical Goal

Extract reusable platform capabilities carefully, without creating a giant dumping ground.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Internal platform libraries | Repeated infrastructure code needs consistency |
| Spring Boot starters | Future services need auto-configured behavior |
| Auto-configuration | Common behavior should be opt-in and conditional |
| Conditional beans | Starters must not override service-specific needs blindly |
| Spring bean lifecycle | Auto-configuration requires lifecycle understanding |
| Spring proxy internals | Transactions, security, and AOP depend on proxies |
| Filter vs interceptor vs aspect | Cross-cutting concerns run at different layers |
| AOP | Some technical policies can be applied declaratively |
| MDC | Logs need request context |
| ThreadLocal | Context propagation must be understood |
| ScopedValue | Safer context propagation with virtual threads |
| Virtual thread context propagation | ThreadLocal assumptions become dangerous |
| Jackson modules | Value objects need consistent serialization |
| Bean Validation groups | Different commands need different validation rules |
| Custom validation annotation | Domain-specific validation becomes reusable |
| ObjectMapper configuration | JSON compatibility must be controlled |
| Exception taxonomy reuse | Error behavior should be consistent |

## Platform Modules

```text
shared/
  common-core/
  common-errors/
  common-web/
  common-logging/
  common-validation/
  common-jackson/
  common-idempotency/
  common-security/
  common-cache/
  common-observability/

platform/
  platform-starters/
    web-starter/
    logging-starter/
    security-starter/
    idempotency-starter/
    observability-starter/
```

## Practical Implementation Tasks

- Create `common-errors` with exception taxonomy and Problem Details mapping.
- Create `common-web` with correlation ID filter and request logging.
- Create `common-logging` with MDC support and JSON logs.
- Create `common-jackson` with serializers for `Money`, IDs, and enums.
- Create `common-validation` with reusable validators.
- Create `common-idempotency` with reusable idempotency service and interceptor.
- Create `common-cache` with cache conventions.
- Create `common-observability` with trace and metric naming helpers.
- Create Spring Boot starters for common modules.
- Add auto-configuration tests using `ApplicationContextRunner`.
- Demonstrate Spring proxy pitfalls:
  - `@Transactional` self-invocation
  - AOP not applied to private methods
  - method security via proxy

## Architectural Decision

### Decision

Create focused shared libraries and starters only when duplication is proven.

### Why

Some concerns are genuinely cross-cutting. But uncontrolled shared code becomes a coupling trap.

### Alternatives Considered

| Alternative | Why Not Enough |
|---|---|
| Copy-paste everywhere | Inconsistent behavior |
| One giant common module | Becomes a dumping ground |
| External platform too early | More process than value |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Consistent behavior | Versioning shared libraries matters |
| Faster service creation | Starters can hide complexity |
| Better governance | Risk of over-abstraction |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Auto-configuration tests | Starters load only when expected |
| Contract tests for common errors | Error shape remains stable |
| Context propagation tests | Correlation ID survives async and virtual-thread boundaries |
| Serialization tests | Money and IDs serialize consistently |

Acceptance criteria:

- A new service can include `web-starter` and get standard errors, logging, and correlation IDs.
- Starters can be disabled or customized.
- No shared module becomes a business-domain dumping ground.

## Staff Engineer Lens

- Shared libraries should reduce cognitive load, not hide critical behavior.
- Platform code needs product thinking too: API, versioning, support, and migration.
- In Java, understanding Spring proxies prevents subtle production bugs.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Starter compatibility contract** — Each starter must document properties, default beans, conditional beans, override points, and migration notes.
- **Auto-configuration metadata** — Add metadata so IDEs expose starter configuration properties clearly.
- **ScopedValue adoption plan** — Prefer explicit context passing or ScopedValue for request context in Java 25 where it is safer than ThreadLocal.
- **AOT/native-image readiness notes** — Do not make native image mandatory, but track reflection, proxies, resources, and serialization assumptions.
- **Shared-library versioning** — Use semantic versioning and changelogs for platform modules once more than one application consumes them.

---
