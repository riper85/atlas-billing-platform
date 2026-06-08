# Phase 13 — Distributed Workflows and Reconciliation

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 13 of 18

## Business Goal

Payment collection becomes a multi-step business process:

1. invoice generated
2. payment authorized
3. fraud check completed
4. payment captured
5. ledger posted
6. email sent
7. subscription activated or suspended

## Technical Goal

Coordinate long-running workflows without distributed transactions.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Saga pattern | Multi-step workflows cross boundaries |
| Choreography | Simple flows can react through events |
| Orchestration | Complex flows need central visibility |
| Compensation | Failed later steps require undo or correction |
| Timeout | Workflows cannot wait forever |
| Retry | Temporary failures should be retried safely |
| Workflow state machine | Long-running process needs explicit states |
| Scheduled recovery job | Stuck workflows need repair |
| Reconciliation job | Source-of-truth consistency must be checked |
| Graceful degradation | Non-critical steps should not block critical flow |
| Manual repair queue | Some failures need human review |
| Business idempotency | Repeated workflow steps must be safe |
| Distributed lock | Rare cases need exclusive ownership across instances |
| Leader election | Only one scheduler/reconciler should run some jobs |

## Workflow States

```text
InvoiceGenerated
PaymentAuthorizationRequested
PaymentAuthorized
FraudCheckPassed
PaymentCaptured
LedgerPosted
NotificationSent
Completed
Suspended
CompensationRequired
Compensated
ManualReviewRequired
```

## Practical Implementation Tasks

- Implement payment saga with Kafka choreography first.
- Add workflow state table:

```text
payment_workflows
  workflow_id
  invoice_id
  payment_id
  state
  version
  last_event_id
  next_retry_at
  created_at
  updated_at
```

- Add lightweight orchestrator when visibility becomes hard.
- Add compensation examples:

```text
PaymentCaptured -> LedgerPostFailed -> PaymentRefundRequested
PaymentAuthorized -> FraudRejected -> AuthorizationVoided
```

- Add timeout handling for payment authorization.
- Add scheduled recovery job for stuck workflows.
- Add reconciliation job comparing invoices, payments, ledger entries, and notifications.
- Add manual review table for unresolved inconsistencies.
- Add leader election for scheduled reconciliation using PostgreSQL advisory lock or ShedLock.
- Use distributed lock only where a single active worker is required.

## Architectural Decision

### Decision

Start with choreography, then add lightweight orchestration when workflow visibility and recovery become difficult.

### Why

Simple event reactions are easy to start with. Complex payment flows need explicit state and recovery.

### Alternatives Considered

| Alternative | Why Not Chosen |
|---|---|
| Distributed transactions | Poor fit for long-running workflows |
| Workflow engine immediately | Hides fundamentals too early and adds heavy dependency |
| Pure choreography forever | Hard to debug and repair complex flows |
| Camunda/Temporal | Useful tools, but intentionally avoided here to learn fundamentals locally |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Clear workflow visibility | More state management |
| Recoverable failures | More operational logic |
| Explicit compensation | Business complexity becomes visible |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Saga success tests | Happy path completes |
| Failure tests | Compensation starts when needed |
| Timeout tests | Stuck workflows transition correctly |
| Reconciliation tests | Inconsistencies are detected |
| Leader election tests | Only one recovery worker performs exclusive work |

Acceptance criteria:

- Payment saga completes successfully.
- Ledger failure triggers compensation path.
- Stuck workflow is detected and recovered.
- Reconciliation creates manual review records for unresolved issues.

## Staff Engineer Lens

- Distributed workflows are mostly about failure management.
- Choreography optimizes decoupling; orchestration optimizes visibility.
- Reconciliation is not optional in serious billing systems.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Workflow ownership map** — For each workflow step, document the owning module, source event, command emitted, timeout, retry policy, and compensation.
- **Saga observability fields** — Carry workflow ID, correlation ID, causation ID, invoice ID, payment ID, and tenant ID through every step.
- **Manual operations model** — Define what support or engineering can safely retry, compensate, ignore, or escalate.
- **Retry schedule visualization** — Add a simple retry timeline for payment authorization, capture, provider callbacks, and ledger posting.
- **Workflow schema evolution** — Plan how workflow state rows survive new states, renamed states, and changed compensation logic.

---
