# Phase 5 — Persistence, Transactions, and Concurrency

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 05 of 18

## Business Goal

Payments, invoices, and ledger entries must remain correct when multiple requests happen at the same time.

## Technical Goal

Learn real transaction boundaries, locking, consistency, migrations, and database performance.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| ACID | Financial operations require atomic consistency |
| Transaction boundary design | Too-wide and too-narrow transactions both create problems |
| Transaction propagation | Nested use cases need clear behavior |
| `@Transactional` self-invocation issue | Spring proxies can be bypassed accidentally |
| Transaction synchronization | Some work must happen after commit |
| Isolation levels | Race conditions depend on database isolation |
| Optimistic locking | Prevent lost updates on subscriptions and invoices |
| Pessimistic locking | Serialize critical payment finalization |
| Deadlocks | Concurrent ledger posting can lock rows in different order |
| Unique constraint race handling | Correctness must survive concurrent inserts |
| Hibernate dirty checking | Entity changes are flushed automatically |
| Persistence context size | Large transactions can consume memory |
| `flush()` behavior | SQL execution timing matters |
| Lazy initialization exception | Entity loading cannot be accidental |
| Fetch join | Solve targeted N+1 cases |
| Entity graph | Control loading without hardcoding every query |
| Batch fetching | Optimize collections |
| DTO projections | Avoid loading full aggregates for read screens |
| Index design | Queries need access paths |
| Composite indexes | Multi-column filters need intentional indexes |
| Partial indexes | Some queries only need active rows |
| Covering indexes | Avoid unnecessary table lookups |
| Keyset pagination | Large invoice lists should not use slow offsets |
| Zero-downtime migration | Schema changes must not require app downtime |
| Expand-contract migration | Old and new code must overlap safely |
| Backfill | Existing data must migrate without blocking the system |
| Database rollback limitation | Data migrations are not as easy to roll back as code |

## Ledger Design

Use an append-only double-entry ledger instead of relying only on mutable balance fields.

```text
ledger_entries
  id
  account_id
  invoice_id
  payment_id
  direction: DEBIT | CREDIT
  amount
  currency
  created_at
  correlation_id
```

## Practical Implementation Tasks

- Create `ledger_accounts` and `ledger_entries` tables.
- Add debit and credit entries in the same transaction.
- Add optimistic locking with `@Version` on subscription and invoice records.
- Add pessimistic locking for payment finalization.
- Add a consistent lock ordering rule to reduce deadlocks.
- Add unique constraints for business invariants.
- Add retry logic for optimistic lock conflicts where safe.
- Add query optimization using `EXPLAIN ANALYZE`.
- Add keyset pagination for invoice listing.
- Implement an expand-contract migration:
  1. expand schema with nullable column
  2. deploy code that writes both old and new fields
  3. backfill old rows
  4. switch reads to new field
  5. contract/remove old field later
- Compare offset pagination vs keyset pagination.

## Architectural Decision

### Decision

Use append-only ledger entries and database-enforced constraints for financial correctness.

### Why

Financial history must be auditable, reconstructable, and resistant to accidental mutation.

### Alternatives Considered

| Alternative | Why Not Yet |
|---|---|
| Mutable balance only | Easy to corrupt and hard to audit |
| Java `synchronized` | Fails with multiple app instances |
| Distributed lock first | Operationally heavy before local DB constraints are exhausted |
| No expand-contract migrations | Causes deployment coupling and downtime risk |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Strong auditability | More complex queries |
| Better correctness | Requires ledger thinking |
| Safer deployments | More migration steps |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Concurrent integration tests | Race conditions do not corrupt payments |
| Migration tests | Flyway migrations work from empty and existing schemas |
| Repository tests | Queries and mappings behave correctly |
| Performance tests | Indexes improve real query plans |
| Deadlock simulation | Lock ordering reduces failures |

Acceptance criteria:

- 50 concurrent payment finalization attempts do not double-post ledger entries.
- Stale updates fail with a clear conflict error.
- Invoice listing uses keyset pagination for large datasets.
- Expand-contract migration is documented in an ADR.

## Staff Engineer Lens

- Database constraints are part of the architecture.
- Correctness beats cleverness in financial systems.
- Zero-downtime migration is a deployment design problem, not only a database task.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Transaction design checklist** — For each use case, document the aggregate touched, transaction boundary, isolation expectation, lock type, and retry behavior.
- **Statement and lock timeouts** — Configure PostgreSQL statement timeout and lock timeout locally so bad queries fail predictably.
- **Database constraint naming convention** — Name constraints consistently so errors can be translated into useful Problem Details.
- **Migration safety checklist** — Every migration states whether it is expand, backfill, switch-read, contract, or rollback-only documentation.
- **`pg_stat_statements` and query baselines** — Track slow or frequent queries before and after indexes or fetch-strategy changes.

---
