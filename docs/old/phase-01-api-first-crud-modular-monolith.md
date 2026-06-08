# Phase 1 — API-First CRUD Modular Monolith

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 01 of 18

## Business Goal

The company needs to onboard customers, define subscription plans, create subscriptions, and generate simple invoices quickly.

## Technical Goal

Build the smallest working product while establishing API and persistence habits that will scale later.

## Scope

One Spring Boot modular-monolith application from the first commit:

```text
apps/billing-app
```

Initial capabilities:

- create customer
- create plan
- create subscription
- list subscriptions
- generate simple invoice

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Spring Boot REST controllers | The product needs HTTP APIs |
| OpenAPI contract | Clients need a stable description of the API |
| DTOs | API models must not expose persistence internals |
| Validation | Bad requests should be rejected at the boundary |
| RFC 7807 Problem Details | Error responses should be consistent from the beginning |
| Global exception handling | Every endpoint should fail in the same shape |
| Spring Data JPA | CRUD persistence is fast to build |
| Hibernate basics | Entity state and dirty checking appear immediately |
| PostgreSQL | The local DB should behave like production-like SQL |
| Flyway | Schema changes must be versioned |
| `@Transactional` | Business operations need atomicity |
| Pagination | List endpoints should not return unbounded data |
| Filtering and sorting | Real clients need useful query APIs |
| Database constraints | Important invariants belong in the database too |
| Testcontainers | Integration tests should use real PostgreSQL, not H2 assumptions |



## Modular Monolith Module Layout From Day One

Even in the first CRUD phase, do not use a generic technical-layer structure as the main organization. Use capability modules immediately:

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

At this stage, the internal boundaries are light, but they exist from the first commit.

## Initial API Design

```http
POST /api/v1/customers
GET  /api/v1/customers/{customerId}
POST /api/v1/plans
GET  /api/v1/plans?page=0&size=20&sort=name,asc
POST /api/v1/subscriptions
GET  /api/v1/customers/{customerId}/subscriptions?page=0&size=20
POST /api/v1/invoices/generate
```

## Practical Implementation Tasks

- Create `CustomerEntity`, `PlanEntity`, `SubscriptionEntity`, and `InvoiceEntity`.
- Create request/response DTOs as Java records.
- Add OpenAPI documentation.
- Add validation annotations.
- Add a global exception handler returning Problem Details.
- Add Flyway migration `V1__init_schema.sql`.
- Add database constraints:
  - not-null constraints
  - unique customer email
  - positive plan price
  - valid subscription status
- Add pagination to list endpoints.
- Add filtering for subscriptions by status.
- Add sorting for plans and invoices.
- Add integration tests with PostgreSQL Testcontainers.

## Architectural Decision

### Decision

Start with one deployable Spring Boot modular monolith and one PostgreSQL database.

### Why

The fastest way to validate the product is to keep deployment, debugging, and transactions simple while still enforcing capability boundaries from day one.

### Alternatives Considered

| Alternative | Why Not Yet |
|---|---|
| Microservices | Domain boundaries are not known yet |
| Kafka-first design | No async pressure exists yet |
| CQRS | Read/write models are simple at this point |
| No OpenAPI | Client integration becomes guesswork |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Fast delivery | Boundaries require early enforcement |
| Easy debugging | One app can become too large later |
| Simple ACID transactions | Scaling is mostly vertical early |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Unit tests | Basic billing calculations work |
| Controller tests | Request validation and error responses work |
| Integration tests | JPA mappings and Flyway schema work on PostgreSQL |
| API contract check | OpenAPI is generated and usable |

Acceptance criteria:

- `mvn verify` passes.
- The app starts with local PostgreSQL.
- OpenAPI UI exposes all endpoints.
- Invalid requests return Problem Details.
- List endpoints are paginated.

## Staff Engineer Lens

- A modular monolith is intentional architecture. The failure mode is an unstructured big ball of mud.
- API habits begin before the first external consumer appears.
- Database constraints are not duplication; they are defense in depth.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Modular monolith from first commit** — Create capability packages immediately: `customer`, `catalog`, `subscription`, `invoice`, `payment`, `ledger`, and `notification`; do not start with generic `controller/service/repository` packages.
- **Spring Modulith baseline** — Add module verification early so module boundaries are visible before the codebase grows.
- **API style guide** — Define URL naming, pagination shape, sorting format, Problem Details extensions, validation error shape, date/time format, and ID format.
- **Java 25 records usage rule** — Use records for request/response DTOs, commands, query filters, event payloads, and lightweight value carriers.
- **OpenAPI linting** — Add a check that rejects undocumented endpoints, inconsistent error responses, and unbounded list endpoints.

---
