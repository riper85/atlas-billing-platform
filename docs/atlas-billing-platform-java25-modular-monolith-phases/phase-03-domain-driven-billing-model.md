# Phase 3 — Domain-Driven Billing Model

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 03 of 18

## Business Goal

Billing rules become more complex:

- free trials
- discounts
- taxes
- subscription suspension
- failed payment retries
- invoice state transitions
- payment state transitions
- account balance corrections

## Technical Goal

Move important business rules out of procedural services and into expressive domain objects.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Bounded context | Billing, catalog, payment, and ledger have different language |
| Ubiquitous language | Engineers and business users need shared terms |
| Aggregate | Invariants need consistency boundaries |
| Entity | Objects with identity evolve over time |
| Value object | Money, IDs, periods, and tax rates need correctness |
| Domain service | Some rules do not naturally belong to one entity |
| Domain event | State changes need to be captured |
| Invariant | Billing correctness requires protected rules |
| State machine thinking | Invoice and payment status transitions must be legal |
| Sealed classes | Java can model limited state hierarchies clearly |
| Records | Commands, IDs, events, and DTOs become immutable |
| Exception taxonomy | Business failures and technical failures should differ |

## Domain Model

```text
Customer
Plan
Subscription
Invoice
Payment
LedgerAccount
LedgerEntry
```

## Value Objects

```text
Money
Currency
CustomerId
SubscriptionId
InvoiceId
PaymentId
TaxRate
BillingPeriod
IdempotencyKey
CorrelationId
```

## Practical Implementation Tasks

- Create immutable `Money`.
- Add currency-safe arithmetic.
- Add `Subscription.activate()`.
- Add `Subscription.cancel()`.
- Add `Subscription.suspendForFailedPayment()`.
- Add `Invoice.markIssued()`.
- Add `Invoice.markPaid()`.
- Add `Invoice.markOverdue()`.
- Add `Payment.authorize()`.
- Add `Payment.capture()`.
- Add `Payment.fail()`.
- Add sealed classes or enums for invoice and payment states.
- Add domain events:

```text
SubscriptionCreated
SubscriptionCancelled
InvoiceGenerated
InvoicePaid
PaymentAuthorized
PaymentSucceeded
PaymentFailed
LedgerEntryPosted
```

- Add a clear exception taxonomy:

```text
BusinessRuleViolationException
ConflictException
NotFoundException
ExternalDependencyException
RetryableTechnicalException
NonRetryableTechnicalException
```

## Architectural Decision

### Decision

Use rich domain models for subscription, invoice, payment, and ledger behavior.

### Why

Billing logic becomes risky when scattered across controllers and procedural service methods.

### Alternatives Considered

| Alternative | Why Not Yet |
|---|---|
| Anemic model | Easy at first, but business rules become procedural spaghetti |
| Rules engine | Too heavy before rule complexity justifies it |
| Stored procedures | Harder to test and refactor as the model evolves |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Better business expressiveness | More mapping code |
| Easier unit testing | Requires modeling discipline |
| Framework-independent rules | More upfront design thinking |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Pure unit tests | Domain rules work without Spring |
| State transition tests | Invalid invoice/payment transitions are rejected |
| Property-style tests | Money calculations preserve invariants |
| Exception tests | Business and technical failures are distinct |

Acceptance criteria:

- Domain tests run without Spring.
- Invalid payment and invoice state transitions fail.
- Money cannot mix currencies accidentally.
- Application services orchestrate domain behavior instead of containing all rules.

## Staff Engineer Lens

- Domain modeling is not academic when money is involved.
- A clean domain model reduces future service extraction risk.
- Invariants should live where they cannot be accidentally skipped.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Temporal modeling** — Model billing periods, invoice issue dates, due dates, grace periods, and subscription effective dates explicitly.
- **Rounding and precision policy** — Define BigDecimal scale, rounding mode, tax rounding strategy, and currency minor-unit behavior.
- **Domain invariant catalog** — Create a living list of invariants per aggregate so tests and database constraints can be traced back to business rules.
- **Specification pattern where useful** — Use explicit domain specifications for rules such as eligibility, suspension, plan change, or invoice generation criteria.
- **Event storming artifact** — Add a lightweight event-storming diagram or table for the billing lifecycle.

---
