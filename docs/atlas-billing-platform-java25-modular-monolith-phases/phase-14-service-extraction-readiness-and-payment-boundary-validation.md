# Phase 14 — Service Extraction Readiness and Payment Boundary Validation

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 14 of 18

## Business Goal

Payment processing changes faster, integrates with external providers, and needs failure isolation from core billing.



## Modular Monolith Default

This phase does **not** mean the project must become microservices. The default architecture remains the Java 25 modular monolith. The exercise is to learn the decision process and mechanics of extracting one module only when the pressure is real.

## Technical Goal

Validate whether the first service should be extracted for a real business and operational reason, while preserving the modular monolith as the default architecture.

## Candidate Extracted Services

```text
billing-platform
payment-service
notification-service
ledger-service
```

Do not extract all at once. Treat this as an optional validation of a proven modular boundary. Start with `payment-service` only if the modular monolith shows real pressure.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Service ownership | Extracted services need clear responsibility |
| Data ownership | Each service owns its data |
| Database per service | Prevent hidden coupling through shared tables |
| API contracts | Services depend on stable APIs |
| Backward compatibility | Independent deployability requires compatibility |
| Pact contract testing | Consumer expectations must be verified |
| WireMock | External services need realistic stubs |
| Service-to-service calls | Some commands remain synchronous |
| Network failure | Remote calls fail differently than method calls |
| Timeout hierarchy | Call chains must respect deadlines |
| Circuit breaker | Repeated failures need containment |
| Bulkhead | One dependency should not consume all resources |
| Fallback | Some failures can degrade gracefully |
| Retry with backoff and jitter | Remote transient failures need controlled retry |
| Outbox per service | Each service needs reliable event publication |
| Inbox per service | Each service needs duplicate protection |
| Strangler fig pattern | Extraction should be incremental |
| Anti-corruption layer migration | New service should protect its model from old module assumptions |
| Feature flags | Traffic migration should be controlled |
| Dark launch | New path can run without affecting users |

## Practical Implementation Tasks

- Create `services/payment-service`.
- Give payment-service its own PostgreSQL database.
- Move payment provider integration to payment-service.
- Keep billing ownership of invoices initially.
- Add REST command API:

```http
POST /api/v1/payments/authorize
POST /api/v1/payments/{paymentId}/capture
POST /api/v1/payments/{paymentId}/refund
```

- Publish payment events through Kafka.
- Add outbox and inbox to payment-service.
- Add Pact contract tests between billing-platform and payment-service.
- Add WireMock tests for payment provider.
- Add Resilience4j:
  - timeout
  - retry
  - circuit breaker
  - bulkhead
  - rate limiter
- Add feature flag for routing payment commands:

```text
payment.routing=modular-monolith | payment-service | shadow
```

- Add dark launch mode where payment-service receives shadow traffic but does not affect user-visible results.
- Add rollback plan to route traffic back to the modular monolith.

## Architectural Decision

### Decision

Make `payment-service` the first extraction candidate, not an automatic destination.

### Why

Payment has external provider integration, retry complexity, failure isolation needs, and different change frequency.

### Alternatives Considered

| Alternative | Why Not Chosen |
|---|---|
| Extract customer first | Low value; mostly CRUD |
| Extract everything | Big-bang distributed complexity |
| Keep all forever | Payment failures can harm core billing reliability |
| Shared database | Easier initially but destroys service independence |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Better failure isolation | Network complexity |
| Independent payment changes | Contract management |
| Clear ownership | Data duplication and eventual consistency |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Pact provider tests | payment-service satisfies billing expectations |
| WireMock tests | Provider failures are handled |
| Resilience tests | Timeouts, retries, and breakers work |
| Feature flag tests | Routing can change safely |
| Shadow traffic tests | New service can be compared before cutover |

Acceptance criteria:

- Billing modular platform can call payment-service.
- Payment-service owns its database.
- Payment events are published through Kafka.
- Feature flag can switch between modular-monolith and service path.
- Rollback path is documented.

## Staff Engineer Lens

- Microservices are not a goal. They are a trade-off.
- Service extraction should follow business pressure and operational isolation.
- The hardest part of microservices is not creating a new app; it is owning compatibility, data, and failure.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Extraction trigger checklist** — Treat extraction as optional: require clear change-rate, failure-isolation, ownership, scaling, or compliance pressure before moving a module out.
- **Modular-monolith fallback path** — Document how to keep or return functionality inside the modular monolith if service extraction adds more cost than value.
- **Data duplication policy** — Define which data is copied across boundaries, source of truth, freshness expectations, and repair strategy.
- **Contract versioning and sunset plan** — Every extracted API and event needs compatibility rules, deprecation headers/events, and a consumer migration window.
- **Shadow comparison metrics** — Compare monolith path and service path results, latency, error rate, and side effects before cutover.

---
