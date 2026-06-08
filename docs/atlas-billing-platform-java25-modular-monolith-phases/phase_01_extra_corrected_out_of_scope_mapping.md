# Phase 01 Extra — Corrected Out-of-Scope Mapping

> **Attach to:** `phase-01-api-first-crud-modular-monolith.md` or the redone Phase 01 file.  
> **Purpose:** Keep Phase 01 focused by showing exactly where intentionally excluded topics will be implemented later.

---

## Explicitly Out of Scope for Phase 01

These topics are important, but they are intentionally not implemented in Phase 01.

| Topic | Why Not in Phase 01 | Where It Belongs |
|---|---|---|
| Real microservice extraction | Domain boundaries are not validated yet. | Phase 14 — Service Extraction Readiness and Payment Boundary Validation |
| Kafka | No real async pressure exists yet. | Phase 11 — Kafka Event Streaming |
| Transactional outbox | Events do not leave the application boundary yet. | Phase 10 — Outbox, Inbox, and Event-Driven Architecture |
| CQRS/read models | Read/write models are still simple. | Phase 09 — Caching, Read Models, and Performance |
| Real payment provider integration | Invoice generation, payment states, idempotency, and workflow reliability must mature first. | Phase 14 Extra — Real Payment Provider Integration Behind Payment Service |
| Double-entry ledger | Accounting correctness is too heavy for the first CRUD slice. | Phase 05 — Persistence, Transactions, and Concurrency |
| Real notification delivery | No reliable async delivery requirement exists yet. | Phase 10 Extra — Reliable Notification Delivery Foundation; Phase 11 Extra — Kafka-Backed Notification Delivery |
| JWT/OAuth2/RBAC | Security deserves its own focused phase. | Phase 08 — Security and Identity |
| Full observability stack | Phase 01 only needs health/info and correlation ID. | Phase 15 — Observability, SLOs, and Incident Readiness |
| Country-specific VAT/tax | Too much domain complexity too early. | Phase 03 Extra — Country Tax Rules, Coupons, Promotions, and Pricing Policies |
| Dynamic pricing/coupons/promotions | The platform only needs simple plans in Phase 01. | Phase 03 Extra — Country Tax Rules, Coupons, Promotions, and Pricing Policies |
| CompletableFuture | Async composition is not justified by basic CRUD. | Phase 18 — Advanced Java Track Inside the Same Product |

---

## Phase 01 Guardrail

Phase 01 may contain module shells, comments, or placeholders for later work, but it should not implement the topics above.

Good Phase 01 behavior:

```text
payment/       shell only
ledger/        shell only
notification/  shell only
identity/      shell only
```

Bad Phase 01 behavior:

```text
Kafka producers/consumers
real payment provider integration
JWT/OAuth2 security implementation
double-entry accounting engine
real email/SMS provider delivery
country-complete VAT engine
full CQRS split
microservice deployment
```

---

## Staff Engineer Lens

Out-of-scope is not a weakness. It is architectural sequencing. A senior engineer does not introduce a technology because it is impressive; they introduce it when the product has created the pressure that makes the trade-off worthwhile.
