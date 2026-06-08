# Phase 01 — API-First CRUD Modular Monolith

> **Target runtime:** Java 25  
> **Architecture stance:** Start with one Spring Boot deployable application, but organize it as a modular monolith from the first commit.  
> **Future direction:** Microservices are the long-term architecture option, but Phase 01 only prepares clean boundaries. It does not extract services yet.  
> **Phase file:** 01 of 18  
> **Main learning goal:** Build the first usable product slice while learning API-first design, Spring Boot application structure, persistence discipline, module boundaries, and basic domain modeling.

---

## 1. Business Goal

The company needs the first working version of a billing platform.

In this phase, the product must be able to:

- onboard customers;
- define subscription plans;
- create subscriptions;
- list and search subscriptions;
- generate a simple invoice;
- expose stable HTTP APIs that future clients can rely on.

The business does **not** yet need real online payments, Kafka, distributed transactions, real tax providers, real notifications, or independent service deployment.

---

## 2. Technical Goal

Build the smallest useful billing application while establishing habits that will still work later if some modules become microservices.

This phase focuses on:

- Spring Boot REST API design;
- OpenAPI-first thinking;
- DTOs and validation;
- consistent error handling;
- PostgreSQL persistence;
- Flyway migrations;
- transactional application services;
- modular-monolith package boundaries;
- Java 25 records and value objects;
- basic architecture tests;
- local development with Testcontainers.

---

## 3. Scope

Create one Spring Boot application:

```text
apps/billing-app
```

The application is one deployable unit, one process, and one PostgreSQL database.

Initial capabilities:

- create customer;
- get customer;
- list customers;
- create plan;
- get plan;
- list plans;
- deactivate plan;
- create subscription;
- get subscription;
- list customer subscriptions;
- cancel subscription;
- generate simple invoice;
- get invoice;
- list invoices;
- expose reference data;
- store basic audit events;
- seed local demo data.

---

## 4. Explicitly Out of Scope for Phase 01

These topics are important, but they are intentionally not implemented in this phase.

| Topic | Why Not in Phase 01 | Suggested Later Phase |
|---|---|---|
| Real microservice extraction | Domain boundaries are not validated yet. | Phase 09 — Microservice Extraction Readiness |
| Kafka | No real async pressure exists yet. | Phase 06 — Async Events and Outbox |
| Transactional outbox | Events do not leave the application boundary yet. | Phase 06 — Async Events and Outbox |
| CQRS | Read/write models are still simple. | Phase 10 or later — Query Model Evolution |
| Real payment provider integration | Invoice generation must exist first. | Phase 07 — Payment Workflow |
| Double-entry ledger | Accounting rules are too heavy for the first CRUD slice. | Phase 08 — Ledger and Accounting |
| Real notification delivery | No reliable delivery requirement yet. | Phase 06 or Phase 11 — Notification Delivery |
| JWT/OAuth2/RBAC | Security deserves its own focused phase. | Phase 04 — Security and Identity |
| Full observability stack | Add only health/info and correlation ID now. | Phase 05 — Observability |
| Country-specific VAT/tax | Too much domain complexity too early. | Later Tax/Billing phase |
| Dynamic pricing/coupons/promotions | The platform only needs simple plans now. | Later Catalog/Pricing phase |

---

## 5. Architecture Decision

### Decision

Start with a **Spring Boot modular monolith** and one PostgreSQL database.

### Why

This gives the team fast delivery, simple local development, simple debugging, and simple ACID transactions while still practicing boundaries that can later support service extraction.

### Alternatives Considered

| Alternative | Why Not Yet |
|---|---|
| Microservices from day one | Too much deployment, coordination, observability, testing, and distributed data complexity before boundaries are known. |
| Kafka-first design | No real asynchronous business pressure exists in the first CRUD slice. |
| CQRS | The read model is not yet complex enough to justify separate read/write models. |
| No OpenAPI | Client integration becomes guesswork and breaking changes become harder to detect. |
| Generic technical packages | `controller/service/repository` packages hide domain boundaries and become a big ball of mud. |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Fast product validation | Boundaries require discipline from the first commit. |
| Easy local debugging | One application can still become too large if boundaries are ignored. |
| Simple transactions | Future extraction requires careful refactoring. |
| One database | Table ownership must be documented to avoid hidden coupling. |

---

## 6. Phase 01 Workable Chunks

Use these chunks as small implementation steps. Finish each chunk before starting the next one.

---

# Chunk 01 — Spring Boot Application and Build Baseline

## Goal

Create the runnable application skeleton and make `mvn verify` meaningful from the start.

## Topics Practiced

- Spring Boot application structure;
- Maven module/reusable build setup;
- Java 25 baseline;
- local profile discipline;
- basic CI-ready verification;
- dependency hygiene.

## Tasks

- Create `apps/billing-app`.
- Configure Java 25.
- Add Spring Boot dependencies for:
  - web;
  - validation;
  - Spring Data JPA;
  - PostgreSQL;
  - Flyway;
  - Actuator;
  - OpenAPI UI;
  - Testcontainers.
- Add local configuration profile:

```text
application.yml
application-local.yml
application-test.yml
```

- Add a basic `/actuator/health` endpoint.
- Add a root `README.md` section explaining how to run Phase 01 locally.

## Done When

- `mvn verify` passes.
- The app starts locally.
- `/actuator/health` returns `UP`.
- Local and test profiles are clearly separated.

---

# Chunk 02 — Platform Baseline

## Goal

Create shared platform behavior once instead of duplicating it in every business module.

## Module

```text
platform/
  web/
  problem/
  validation/
  pagination/
  time/
  config/
```

## Topics Practiced

- Spring Boot filters;
- RFC 7807 Problem Details;
- global exception handling;
- correlation ID;
- configuration properties;
- Java `Clock` injection;
- consistent pagination model.

## Tasks

- Add a correlation ID filter.
- Accept incoming `X-Correlation-ID` if present.
- Generate a correlation ID if missing.
- Include correlation ID in:
  - logs;
  - Problem Details responses;
  - response headers.
- Add a global exception handler.
- Standardize Problem Details extensions:

```json
{
  "type": "https://docs.atlas.local/problems/customer-email-already-exists",
  "title": "Customer email already exists",
  "status": 409,
  "detail": "A customer with this email already exists.",
  "errorCode": "CUSTOMER_EMAIL_ALREADY_EXISTS",
  "correlationId": "..."
}
```

- Add a public error code catalog.
- Add a reusable page response model.
- Add a `Clock` bean.
- Add typed `@ConfigurationProperties` for billing settings.
- Validate configuration at startup.

## Missing Concepts Added Here

- Correlation ID baseline;
- Problem Details extensions;
- public error code catalog;
- config validation;
- clock injection;
- pagination response standard.

## Done When

- Invalid requests return Problem Details.
- Every error response has `errorCode` and `correlationId`.
- Every response includes `X-Correlation-ID`.
- Time-sensitive logic can use injected `Clock`.

---

# Chunk 03 — Modular Monolith Boundaries

## Goal

Create capability modules from the first commit so the application does not become a generic layered monolith.

## Main Module Layout

```text
apps/billing-app/src/main/java/com/atlas/billing/
  platform/
    web/
    problem/
    validation/
    pagination/
    time/
    config/

  customer/
    api/
    application/
    domain/
    infrastructure/

  catalog/
    api/
    application/
    domain/
    infrastructure/

  pricing/
    api/
    application/
    domain/
    infrastructure/

  tax/
    api/
    application/
    domain/
    infrastructure/

  subscription/
    api/
    application/
    domain/
    infrastructure/

  invoice/
    api/
    application/
    domain/
    infrastructure/

  audit/
    api/
    application/
    domain/
    infrastructure/

  referencedata/
    api/
    application/
    domain/
    infrastructure/

  bootstrap/
    local/

  identityaccess/
    api/
    internal/

  admin/
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

## Package Meaning

| Package | Meaning |
|---|---|
| `api` | Public module-facing types: controllers, module API interfaces, request/response models. |
| `application` | Use cases, orchestration, transactions, command/query handlers. |
| `domain` | Business concepts, value objects, invariants, domain services. |
| `infrastructure` | JPA entities, repositories, database adapters, persistence mapping. |
| `internal` | Placeholder module internals for modules not implemented yet. |

## Boundary Rules

- Controllers call application services, not repositories.
- `@Transactional` belongs on application service methods, not controllers.
- One module must not call another module's repository.
- One module must not use another module's JPA entity.
- Cross-module communication uses IDs, small DTOs, or public module APIs.
- Package-private classes should be preferred inside module internals where practical.
- Public APIs should be intentionally small.

## Suggested Dependency Direction

```text
subscription -> customer
subscription -> catalog
subscription -> pricing
invoice      -> subscription
invoice      -> pricing
invoice      -> tax
invoice      -> customer
catalog      -> pricing
```

Avoid reverse dependencies unless there is a strong reason.

## Missing Concepts Added Here

- No cross-module repository access;
- module-owned persistence rule;
- dependency direction rule;
- microservice extraction preparation;
- public module API contracts;
- avoiding shared domain models.

## Done When

- The module package structure exists.
- Implemented modules use `api/application/domain/infrastructure`.
- Placeholder modules use only `api/internal`.
- Architecture tests prevent repository/entity access across module boundaries.

---

# Chunk 04 — API Style Guide and OpenAPI Contract

## Goal

Make the API consistent before there are many endpoints.

## Topics Practiced

- REST resource naming;
- OpenAPI documentation;
- HTTP status discipline;
- request/response DTOs;
- Java records;
- validation;
- API versioning;
- stable error response shape.

## API Style Rules

- Use `/api/v1` for all public endpoints.
- Use plural nouns for resources.
- Use nested URLs only when ownership is clear.
- Never expose JPA entities in API responses.
- Use Java records for request and response DTOs.
- Use ISO-8601 for dates and timestamps.
- Store timestamps in UTC.
- Use stable IDs in API responses.
- Return `201 Created` and `Location` for successful create operations.
- Return `409 Conflict` for business conflicts.
- Return `400 Bad Request` for malformed input.
- Return `422 Unprocessable Entity` only if you decide to distinguish semantic validation from syntax validation.
- Return paginated responses for list endpoints.

## Initial API Design

```http
POST   /api/v1/customers
GET    /api/v1/customers/{customerId}
GET    /api/v1/customers?page=0&size=20&sort=createdAt,desc
PATCH  /api/v1/customers/{customerId}

POST   /api/v1/plans
GET    /api/v1/plans/{planId}
GET    /api/v1/plans?page=0&size=20&sort=name,asc
POST   /api/v1/plans/{planId}/deactivate

POST   /api/v1/subscriptions
GET    /api/v1/subscriptions/{subscriptionId}
GET    /api/v1/customers/{customerId}/subscriptions?page=0&size=20&status=ACTIVE
POST   /api/v1/subscriptions/{subscriptionId}/cancel

POST   /api/v1/invoices/generate
GET    /api/v1/invoices/{invoiceId}
GET    /api/v1/invoices?page=0&size=20&sort=createdAt,desc

GET    /api/v1/reference-data/currencies
GET    /api/v1/reference-data/billing-periods
GET    /api/v1/reference-data/subscription-statuses
```

## Idempotency Rule

Unsafe create/generate operations should accept an optional `Idempotency-Key` header:

```http
Idempotency-Key: 9f83c0c2-39f9-4c45-a5cc-5b2e5a54d3ac
```

Use it for:

- `POST /api/v1/customers`;
- `POST /api/v1/subscriptions`;
- `POST /api/v1/invoices/generate`.

In Phase 01, keep the implementation simple:

- store the key with operation type and result reference;
- prevent duplicate execution for the same key;
- return the same resource/result on retry.

## Missing Concepts Added Here

- HTTP status discipline;
- `Location` header;
- idempotency keys;
- API versioning rule;
- request/response examples;
- OpenAPI linting idea;
- validation error shape.

## Done When

- OpenAPI UI exposes all endpoints.
- Create endpoints return `201 Created` with `Location`.
- Business conflicts return `409 Conflict`.
- List endpoints are paginated.
- API examples exist for success, validation failure, and conflict responses.

---

# Chunk 05 — Customer Module

## Goal

Implement customer onboarding with validation, persistence, conflict handling, and clean module boundaries.

## Module

```text
customer/
  api/
  application/
  domain/
  infrastructure/
```

## Topics Practiced

- DTO records;
- application services;
- JPA entity mapping;
- validation;
- unique constraints;
- business conflict modeling;
- value objects;
- audit fields;
- idempotent create.

## Domain Concepts

- `CustomerId`;
- `EmailAddress`;
- `CustomerStatus`:
  - `ACTIVE`;
  - `DISABLED`.

## APIs

```http
POST  /api/v1/customers
GET   /api/v1/customers/{customerId}
GET   /api/v1/customers?page=0&size=20&sort=createdAt,desc
PATCH /api/v1/customers/{customerId}
```

## Tasks

- Create `CustomerEntity`.
- Add fields:
  - id;
  - name;
  - email;
  - status;
  - createdAt;
  - updatedAt;
  - createdBy;
  - updatedBy.
- Add unique database constraint on email.
- Add `CreateCustomerRequest` record.
- Add `CustomerResponse` record.
- Add `CreateCustomerCommand` record.
- Add `EmailAddress` value object.
- Add duplicate email handling as `409 Conflict`.
- Add idempotency support for customer creation.
- Add pagination for customer listing.

## Java Advanced Features Used Here

- Records for DTOs and commands;
- value objects to avoid primitive obsession;
- enum persisted as string;
- optional sealed result type for business outcomes:

```java
sealed interface CreateCustomerResult {
    record Created(CustomerId customerId) implements CreateCustomerResult {}
    record EmailAlreadyExists(EmailAddress email) implements CreateCustomerResult {}
}
```

## Done When

- Customer creation works.
- Duplicate email returns `409 Conflict`.
- Customer listing is paginated.
- Customer DTOs do not expose JPA entities.
- Customer create is safe to retry with the same idempotency key.

---

# Chunk 06 — Catalog, Pricing, and Reference Data

## Goal

Create subscription plans while keeping money, currency, and billing-period rules clean from the beginning.

## Modules

```text
catalog/
  api/
  application/
  domain/
  infrastructure/

pricing/
  api/
  application/
  domain/
  infrastructure/

referencedata/
  api/
  application/
  domain/
  infrastructure/
```

## Topics Practiced

- plan modeling;
- money modeling;
- BigDecimal rules;
- reference data;
- database constraints;
- enum persistence;
- module collaboration without repository sharing.

## Catalog Domain Concepts

- `PlanId`;
- `PlanStatus`:
  - `DRAFT`;
  - `ACTIVE`;
  - `INACTIVE`;
- `BillingPeriod`:
  - `MONTHLY`;
  - `YEARLY`.

## Pricing Domain Concepts

- `Money`;
- `CurrencyCode`;
- `PriceAmount`;
- rounding rule;
- scale rule.

## Reference Data Concepts

- supported currencies;
- billing periods;
- subscription statuses;
- invoice statuses.

## APIs

```http
POST /api/v1/plans
GET  /api/v1/plans/{planId}
GET  /api/v1/plans?page=0&size=20&sort=name,asc
POST /api/v1/plans/{planId}/deactivate

GET  /api/v1/reference-data/currencies
GET  /api/v1/reference-data/billing-periods
GET  /api/v1/reference-data/subscription-statuses
```

## Tasks

- Create `PlanEntity`.
- Store plan amount and currency.
- Add check constraint for positive price.
- Add enum string persistence for status and billing period.
- Add `Money` value object.
- Define BigDecimal scale and rounding rule.
- Add supported currency validation.
- Add plan creation API.
- Add plan listing API.
- Add plan deactivation API.
- Prevent inactive plans from being used for new subscriptions.

## Missing Concepts Added Here

- Money value object;
- BigDecimal scale and rounding;
- currency rule;
- billing period rule;
- reference-data module;
- plan status transition.

## Done When

- Plans can be created and listed.
- Plan price cannot be negative or zero.
- Currency is explicit.
- Plan status is enforced.
- Reference data endpoints return supported values.

---

# Chunk 07 — Subscription Module

## Goal

Create subscriptions while enforcing real business invariants.

## Module

```text
subscription/
  api/
  application/
  domain/
  infrastructure/
```

## Topics Practiced

- transactional application services;
- aggregate boundaries;
- business invariants;
- state transitions;
- conflict handling;
- module collaboration;
- read-only transactions.

## Domain Concepts

- `SubscriptionId`;
- `SubscriptionStatus`:
  - `PENDING`;
  - `ACTIVE`;
  - `CANCELLED`;
  - `EXPIRED`.

## APIs

```http
POST /api/v1/subscriptions
GET  /api/v1/subscriptions/{subscriptionId}
GET  /api/v1/customers/{customerId}/subscriptions?page=0&size=20&status=ACTIVE
POST /api/v1/subscriptions/{subscriptionId}/cancel
```

## Business Rules

- A subscription must reference an existing customer.
- A subscription must reference an active plan.
- A customer cannot have two active subscriptions for the same plan.
- Cancelled subscriptions cannot be cancelled again.
- List endpoints must be paginated.
- Filtering by status must be supported.

## Tasks

- Create `SubscriptionEntity`.
- Add database constraints for valid status.
- Add index on customer ID.
- Add unique business protection for active subscription if practical.
- Add `CreateSubscriptionRequest` record.
- Add `CreateSubscriptionCommand` record.
- Add `SubscriptionResponse` record.
- Add transactional create use case.
- Add read-only transaction for query use cases.
- Add cancellation use case.
- Add duplicate active subscription conflict.
- Add idempotency support for subscription creation.

## Missing Concepts Added Here

- Aggregate boundary thinking;
- active subscription invariant;
- state transition rules;
- application-service transaction boundary;
- read-only transaction discipline;
- business conflict modeling.

## Done When

- Subscription creation works.
- Duplicate active subscription returns `409 Conflict`.
- Inactive plans cannot be used.
- Subscription cancellation follows state rules.
- Query endpoints use pagination and filtering.

---

# Chunk 08 — Invoice, Tax, and Simple Billing Calculation

## Goal

Generate a simple invoice from an active subscription while keeping invoice calculation deterministic and testable.

## Modules

```text
invoice/
  api/
  application/
  domain/
  infrastructure/

tax/
  api/
  application/
  domain/
  infrastructure/
```

## Topics Practiced

- invoice aggregate modeling;
- deterministic time with `Clock`;
- invoice lines;
- tax calculation boundary;
- totals calculation;
- idempotent invoice generation;
- transaction rollback behavior.

## Invoice Domain Concepts

- `InvoiceId`;
- `InvoiceNumber`;
- `InvoiceStatus`:
  - `DRAFT`;
  - `ISSUED`;
  - `VOID`;
- `InvoiceLine`;
- `InvoiceTotal`.

## Tax Domain Concepts

- `TaxRate`;
- `TaxAmount`;
- zero-tax strategy;
- fixed-tax strategy for local learning.

## APIs

```http
POST /api/v1/invoices/generate
GET  /api/v1/invoices/{invoiceId}
GET  /api/v1/invoices?page=0&size=20&sort=createdAt,desc
```

## Business Rules

- Invoice generation requires an active subscription.
- Invoice total must be calculated from line items.
- Tax must be explicit, even if the rate is zero.
- Invoice generation must use injected `Clock`.
- Invoice generation must be idempotent for retry safety.
- Invoice status starts as `ISSUED` for the simple Phase 01 flow.

## Tasks

- Create `InvoiceEntity`.
- Create `InvoiceLineEntity` or embedded line representation.
- Add invoice number generation.
- Add tax line calculation.
- Add total calculation.
- Add `GenerateInvoiceRequest` record.
- Add `InvoiceResponse` record.
- Add idempotency support for invoice generation.
- Add sorting by created date.
- Add fixed-clock tests.
- Add rollback test for failed invoice generation.

## Missing Concepts Added Here

- Invoice line items;
- invoice number;
- tax module boundary;
- fixed/zero tax strategy;
- clock injection;
- deterministic time tests;
- transaction rollback tests.

## Done When

- Invoice generation works for an active subscription.
- Generated invoice contains line items, tax, and total.
- Invoice generation is deterministic in tests.
- Retrying invoice generation with the same idempotency key does not create duplicates.

---

# Chunk 09 — Audit and Bootstrap

## Goal

Make the app usable locally and make important business actions traceable.

## Modules

```text
audit/
  api/
  application/
  domain/
  infrastructure/

bootstrap/
  local/
```

## Topics Practiced

- audit trail basics;
- local demo data;
- Spring profiles;
- startup initialization;
- separating local/test/demo behavior from production behavior.

## Audit Events

Record simple audit events for:

- customer created;
- plan created;
- plan deactivated;
- subscription created;
- subscription cancelled;
- invoice generated.

## Bootstrap Data

Seed only in the `local` profile:

- sample monthly plan;
- sample yearly plan;
- sample customer;
- sample active subscription.

## Tasks

- Create `AuditEventEntity`.
- Create `AuditEventType` enum.
- Store:
  - event ID;
  - event type;
  - aggregate type;
  - aggregate ID;
  - actor;
  - timestamp;
  - correlation ID.
- Add simple audit service.
- Call audit service from application use cases.
- Add local-only bootstrap configuration.
- Make bootstrap idempotent.

## Missing Concepts Added Here

- Audit module baseline;
- audit fields;
- actor placeholder;
- local profile seed data;
- idempotent bootstrap.

## Done When

- Important business actions create audit records.
- Local demo data is available after startup.
- Bootstrap does not run in test/production-like profiles unless explicitly enabled.

---

# Chunk 10 — Identity, Admin, Payment, Ledger, and Notification Shells

## Goal

Keep future modules visible without implementing premature complexity.

These modules exist in Phase 01 only as architectural placeholders.

```text
identityaccess/
  api/
  internal/

admin/
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

## Identity Access Shell

Add only:

- `ActorId`;
- `CurrentActor` interface;
- `SystemActor` implementation;
- no real authentication;
- no JWT;
- no OAuth2;
- no RBAC.

Full implementation belongs in **Phase 04 — Security and Identity**.

## Admin Shell

Add only:

- package placeholder;
- optional local-only admin listing endpoint if useful;
- no role protection yet.

Full implementation belongs after identity exists.

## Payment Shell

Add only:

- package placeholder;
- optional `PaymentIntentId` type;
- no external payment provider;
- no webhooks;
- no retries.

Full implementation belongs in **Phase 07 — Payment Workflow**.

## Ledger Shell

Add only:

- package placeholder;
- optional accounting note from invoice generation;
- no double-entry accounting.

Full implementation belongs in **Phase 08 — Ledger and Accounting**.

## Notification Shell

Add only:

- package placeholder;
- optional in-process listener that logs `InvoiceGenerated`;
- no email;
- no SMS;
- no Kafka;
- no retries.

Full implementation belongs in **Phase 06 — Async Events and Outbox** or a later notification phase.

## Done When

- Future modules exist but do not create fake complexity.
- Their README/package notes explain what is intentionally deferred.

---

# Chunk 11 — Persistence and Flyway

## Goal

Use PostgreSQL as the source of truth for core constraints.

## Topics Practiced

- Flyway migration discipline;
- JPA entity mapping;
- database constraints;
- indexes;
- enum string persistence;
- optimistic future schema evolution.

## Tasks

Create migration:

```text
V1__init_schema.sql
```

Minimum tables:

- `customer_customers`;
- `catalog_plans`;
- `subscription_subscriptions`;
- `invoice_invoices`;
- `invoice_invoice_lines`;
- `audit_events`;
- `platform_idempotency_keys`.

Add constraints:

- not-null customer name and email;
- unique customer email;
- positive plan price;
- supported plan status;
- supported billing period;
- supported subscription status;
- supported invoice status;
- not-null invoice total;
- unique idempotency key per operation scope.

Add indexes:

- customer email;
- subscription customer ID;
- subscription status;
- invoice customer ID if stored;
- invoice created date;
- audit aggregate ID;
- idempotency key.

## Done When

- Flyway runs cleanly on empty PostgreSQL.
- JPA mappings match the schema.
- Database constraints reject invalid data even if Java validation is bypassed.

---

# Chunk 12 — Testing and Architecture Verification

## Goal

Prove the first slice works and prevent the most common architectural shortcuts.

## Test Types

| Test Type | What It Proves |
|---|---|
| Unit tests | Value objects, billing calculations, state transitions. |
| Controller tests | Validation, HTTP status codes, Problem Details. |
| Integration tests | JPA mappings, Flyway, PostgreSQL constraints. |
| Application service tests | Transactions, business conflicts, rollback behavior. |
| Architecture tests | Module boundaries are respected. |
| API contract checks | OpenAPI exists and remains consistent. |

## Required Tests

- Create customer success.
- Duplicate customer email returns `409 Conflict`.
- Invalid email returns Problem Details.
- Create plan success.
- Negative plan price rejected by validation and database constraint.
- Create subscription success.
- Duplicate active subscription returns `409 Conflict`.
- Cannot subscribe to inactive plan.
- Cancel subscription success.
- Generate invoice success.
- Invoice total calculation test.
- Invoice generation with fixed clock.
- Idempotent retry does not create duplicate customer/subscription/invoice.
- List endpoints are paginated.
- Sorting works for plans and invoices.
- Architecture test prevents cross-module repository access.

## Tools

- JUnit 5;
- AssertJ;
- Spring Boot Test;
- Testcontainers PostgreSQL;
- MockMvc or WebTestClient;
- ArchUnit or Spring Modulith verification.

## Done When

- `mvn verify` passes.
- Testcontainers run against real PostgreSQL.
- Architecture rules are executable tests, not only comments.

---

## 7. Concepts Introduced Naturally in Phase 01

| Concept | Where It Appears | Why It Appears Now |
|---|---|---|
| Spring Boot REST controllers | Chunks 04-08 | The product needs HTTP APIs. |
| OpenAPI contract | Chunk 04 | Clients need stable API documentation. |
| DTO records | Chunks 04-08 | API models must not expose persistence internals. |
| Bean Validation | Chunks 04-08 | Bad requests should be rejected at the boundary. |
| RFC 7807 Problem Details | Chunk 02 | Errors should have one consistent shape. |
| Global exception handling | Chunk 02 | Every endpoint should fail consistently. |
| Correlation ID | Chunk 02 | Requests need traceability from the first API call. |
| Spring Data JPA | Chunks 05-08 | CRUD persistence is fast and useful here. |
| Hibernate basics | Chunks 05-08 | Entity state, dirty checking, and lazy loading appear immediately. |
| PostgreSQL | Chunk 11 | Local DB should behave like production-like SQL. |
| Flyway | Chunk 11 | Schema changes must be versioned. |
| `@Transactional` | Chunks 05-08 | Business operations need atomicity. |
| Read-only transactions | Chunks 05-08 | Query use cases should communicate intent. |
| Pagination | Chunks 04-08 | List endpoints must not be unbounded. |
| Filtering and sorting | Chunks 04, 07, 08 | Real clients need useful query APIs. |
| Database constraints | Chunk 11 | Important invariants belong in the database too. |
| Testcontainers | Chunk 12 | Integration tests should use real PostgreSQL, not H2 assumptions. |
| Modular monolith boundaries | Chunk 03 | Microservice-readiness starts with clean module boundaries. |
| Architecture tests | Chunk 12 | Boundaries should be enforced automatically. |
| Java records | Chunks 04-08 | DTOs, commands, and query filters should be immutable. |
| Value objects | Chunks 05-08 | Important business primitives should not be raw strings/numbers everywhere. |
| Sealed result types | Optional in Chunks 05-07 | Business outcomes can be modeled explicitly. |
| BigDecimal money rules | Chunk 06 | Billing cannot treat money as a casual number. |
| Clock injection | Chunks 02 and 08 | Time-sensitive logic must be testable. |
| Idempotency keys | Chunks 04, 05, 07, 08 | Client retries should not create duplicate records. |
| Audit events | Chunk 09 | Important business actions must be traceable. |

---

## 8. Concept Allocation Matrix

Use this table when you are unsure whether to add a concept now or later.

| Concept | Phase 01 Decision | Later Phase |
|---|---|---|
| Modular monolith | Implement now. | Strengthen in Phase 02. |
| Spring Modulith or ArchUnit | Add basic verification now. | Expand in Phase 02. |
| API style guide | Implement now. | Refine continuously. |
| Java records | Implement now. | Continue throughout all phases. |
| Java sealed interfaces | Optional now for business outcomes. | Use more deeply in domain modeling phase. |
| Pattern matching | Optional now. | Use naturally when sealed outcomes grow. |
| Virtual threads | Mention only; do not force yet. | Add when concurrent/parallel work appears. |
| CompletableFuture | Do not use yet. | Add only when async composition is justified. |
| Kafka | Do not use yet. | Phase 06. |
| Outbox | Do not use yet. | Phase 06. |
| JWT/OAuth2 | Do not use yet. | Phase 04. |
| RBAC | Do not use yet. | Phase 04. |
| Real payment provider | Do not use yet. | Phase 07. |
| Ledger | Shell only. | Phase 08. |
| Notification delivery | Shell only. | Phase 06 or later. |
| Observability stack | Only health/info and correlation ID now. | Phase 05. |
| CQRS | Do not use yet. | Later read-model phase. |
| Microservice extraction | Do not extract yet. | Phase 09 or later. |

---

## 9. Final Acceptance Gate for Phase 01

Phase 01 is complete only when all of the following are true:

- `mvn verify` passes.
- The app starts with local PostgreSQL.
- Flyway creates the schema from scratch.
- OpenAPI UI exposes all Phase 01 endpoints.
- Invalid requests return RFC 7807 Problem Details.
- Error responses include `errorCode` and `correlationId`.
- Create endpoints return `201 Created` and `Location`.
- List endpoints are paginated.
- Customer email uniqueness is enforced by Java and PostgreSQL.
- Plan price positivity is enforced by Java and PostgreSQL.
- Subscription business conflicts return `409 Conflict`.
- Invoice generation works and is deterministic in tests.
- Idempotency keys prevent duplicate customer/subscription/invoice creation.
- Audit records are created for important business actions.
- Testcontainers run integration tests against PostgreSQL.
- Architecture tests prevent cross-module repository/entity access.
- Placeholder modules exist but do not implement premature complexity.

---

## 10. Staff Engineer Lens

- A modular monolith is intentional architecture, not a compromise.
- The first failure mode to avoid is not “lack of microservices”; it is an unstructured monolith.
- API discipline starts before the first external consumer appears.
- Database constraints are not duplication; they are defense in depth.
- Transactions are simple now, but boundaries should not require distributed transactions later.
- Future microservices begin as clear module ownership, not as separate deployments.
- Async, Kafka, payments, ledger, and security should appear when the business problem requires them, not because they are fashionable.
- Phase 01 should feel boring operationally and disciplined architecturally.

---

## 11. Suggested Implementation Order Summary

```text
01. Create Spring Boot app and build baseline
02. Add platform baseline: Problem Details, correlation ID, Clock, config
03. Create module/package boundaries
04. Define API style guide and OpenAPI contract
05. Implement customer module
06. Implement catalog, pricing, and reference data
07. Implement subscription module
08. Implement invoice and tax module
09. Implement audit and bootstrap data
10. Add placeholder shells for future modules
11. Add Flyway schema and constraints
12. Add tests and architecture verification
```

Do not start the next phase until this phase is boring, repeatable, and fully verified locally.
