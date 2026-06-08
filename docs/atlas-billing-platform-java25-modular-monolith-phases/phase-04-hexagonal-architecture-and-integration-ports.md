# Phase 4 — Hexagonal Architecture and Integration Ports

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 04 of 18

## Business Goal

The company expects future changes:

- different payment providers
- different notification providers
- REST and event-based inputs
- different persistence approaches
- integrations with legacy finance systems

## Technical Goal

Decouple business behavior from frameworks, persistence, messaging, and external systems.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Ports and adapters | The domain should not depend on infrastructure |
| Dependency inversion | Business rules should define what they need |
| Inbound adapter | REST, scheduled jobs, and events trigger use cases |
| Outbound adapter | Databases and providers implement domain needs |
| Application service | Use cases coordinate transactions and ports |
| Anti-corruption layer | External models should not leak into the domain |
| Persistence mapper | JPA entities should not become the domain model |
| Command object | Use cases should receive clear intent |
| Query object | Reads should be explicit and optimized independently |
| MapStruct | Mapping becomes repetitive and needs consistency |
| Application events vs domain events | Internal notifications differ from business facts |

## Target Module Structure

```text
subscription/
  domain/
  application/
    command/
    query/
    service/
  port/
    in/
    out/
  adapter/
    in/
      web/
      messaging/
      scheduler/
    out/
      postgres/
      paymentprovider/
      notification/
```

## Practical Implementation Tasks

Create inbound ports:

```text
CreateSubscriptionUseCase
CancelSubscriptionUseCase
GenerateInvoiceUseCase
RegisterPaymentResultUseCase
```

Create outbound ports:

```text
LoadCustomerPort
LoadPlanPort
SaveSubscriptionPort
SaveInvoicePort
PaymentProviderPort
LedgerPostingPort
NotificationPort
```

Implement adapters:

- REST controllers as inbound adapters.
- Scheduled invoice generation as inbound adapter.
- JPA repositories as outbound adapters.
- Payment provider stub as outbound adapter.
- Notification stub as outbound adapter.
- MapStruct mappers for DTO/domain/entity conversion.
- Anti-corruption layer for payment provider response mapping.

## Architectural Decision

### Decision

Use hexagonal architecture inside important modules, not blindly everywhere.

### Why

The payment, invoice, subscription, and ledger areas have real reasons to change independently from infrastructure.

### Alternatives Considered

| Alternative | Why Not Yet |
|---|---|
| Traditional controller-service-repository everywhere | Framework and persistence concerns leak into business logic |
| Full clean architecture with many rings | Too much ceremony for simple modules |
| JPA entities as domain | Faster early, but couples persistence to business rules |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Replaceable infrastructure | More classes |
| Testable business logic | More mapping |
| Clear use cases | Slower initial coding |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Use-case tests | Application logic works through ports |
| Adapter tests | Persistence and provider adapters behave correctly |
| Mapper tests | DTO/domain/entity conversions are safe |
| Architecture tests | Dependencies point inward |

Acceptance criteria:

- Domain code imports no Spring, JPA, Kafka, or HTTP classes.
- External provider DTOs do not leak into domain objects.
- Use cases can be tested with fake ports.

## Staff Engineer Lens

- Hexagonal architecture is valuable when change pressure exists.
- Ports are not abstractions for their own sake; they protect business policy.
- Anti-corruption layers are essential when integrating with external systems.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Primary vs secondary ports** — Label inbound ports as use-case boundaries and outbound ports as dependencies required by the use case.
- **Adapter contract tests** — Test adapters against port expectations, not only against implementation details.
- **Port error semantics** — Define which errors are business conflicts, retryable technical failures, non-retryable failures, and integration mapping failures.
- **Boundary DTO rule** — Do not pass HTTP DTOs, Kafka payloads, provider payloads, or JPA entities through domain or application ports.
- **Anti-corruption mapping tests** — Add tests proving external payment-provider statuses map safely into internal payment states.

---
