# Phase 10 — Outbox, Inbox, and Event-Driven Architecture

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 10 of 18

## Business Goal

Invoice creation should trigger follow-up work without making the API wait for everything:

- payment attempt
- ledger posting
- notification
- analytics update
- reporting projection update

## Technical Goal

Introduce asynchronous communication safely before adding Kafka complexity.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Domain event | Domain state changes need to be represented |
| Integration event | Other modules/services need stable event contracts |
| Transactional outbox | Avoid dual-write between DB and messaging |
| Transactional inbox | Consumers must deduplicate incoming events |
| Event envelope | Metadata should be standard on every event |
| Event ID | Consumers need deduplication identity |
| Correlation ID | Async work must connect to the original request |
| Causation ID | Events should reference what caused them |
| Tenant ID | Multi-tenant events need ownership context |
| Event version | Event contracts evolve over time |
| At-least-once delivery | Duplicate delivery is normal |
| Idempotent event handler | Consumers must handle duplicates safely |
| Poison message handling | Bad messages should not block all processing |
| Retry table | Failed work needs controlled retry |
| Dead-letter table | Permanently failed work needs investigation |
| Scheduled recovery job | Stuck events need repair |
| Reconciliation job | Async state needs verification against source of truth |
| Audit log vs event log | Not every event is an audit record |

## Event Envelope

```json
{
  "eventId": "uuid",
  "eventType": "billing.invoice.generated",
  "eventVersion": 1,
  "tenantId": "tenant-123",
  "aggregateType": "Invoice",
  "aggregateId": "invoice-123",
  "correlationId": "corr-123",
  "causationId": "cmd-123",
  "occurredAt": "2026-06-04T10:15:30Z",
  "payload": {}
}
```

## Practical Implementation Tasks

- Create `outbox_events` table.
- Store integration events in the same transaction as invoice/payment changes.
- Create `inbox_events` table for consumer deduplication.
- Add outbox poller.
- Add event dispatcher.
- Add idempotent consumers for:
  - notification
  - reporting projection
  - ledger posting
- Add retry table or retry fields.
- Add dead-letter table.
- Add poison message classification.
- Add scheduled recovery job for stuck `IN_PROGRESS` events.
- Add reconciliation job that verifies:
  - paid invoices have payment records
  - successful payments have ledger entries
  - sent notifications match expected events

## Architectural Decision

### Decision

Use the transactional outbox and inbox patterns before Kafka.

### Why

The team must understand reliable event publication and duplicate handling before introducing distributed streaming infrastructure.

### Alternatives Considered

| Alternative | Why Not Enough |
|---|---|
| Publish directly to Kafka inside DB transaction | Dual-write risk |
| XA transactions | Heavy and uncommon for this local-first system |
| Ignore duplicates | Unsafe for billing |
| Kafka first | Hides the core consistency problem |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Reliable event publication | More tables |
| Duplicate-safe consumers | More handler logic |
| Better recovery | Operational jobs required |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Outbox transaction tests | Data and events commit atomically |
| Inbox deduplication tests | Duplicate events do not duplicate side effects |
| Poison message tests | Bad events are isolated |
| Recovery job tests | Stuck events are retried or marked failed |
| Reconciliation tests | Async state can be checked |

Acceptance criteria:

- Invoice generation writes invoice and outbox event in one transaction.
- Duplicate event delivery does not duplicate notifications or ledger entries.
- Poison events move to dead-letter storage.
- Reconciliation detects intentionally introduced inconsistencies.

## Staff Engineer Lens

- Async architecture begins with correctness, not Kafka.
- Every consumer must be idempotent because at-least-once delivery is normal.
- Reconciliation is how financial systems regain confidence after partial failure.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Event catalog** — Document every integration event with owner, schema, version, producer, consumers, retention, and compatibility expectation.
- **Module events before distributed events** — Inside the modular monolith, distinguish local module events from events that are safe to publish outside the process.
- **Consumer contract for events** — Write tests that prove consumers tolerate optional fields, unknown enum values where appropriate, and duplicate delivery.
- **Outbox retention policy** — Define retention, archival, and cleanup for published and dead-lettered outbox rows.
- **Operational replay procedure** — Document how to replay a dead-lettered or previously published event safely.

---
