# Phase 12 — CDC with Debezium

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 12 of 18

## Business Goal

Outbox polling starts adding database load and event publishing latency.

## Technical Goal

Use PostgreSQL WAL through Debezium to extract outbox events more efficiently.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| PostgreSQL WAL | Source of committed database changes |
| Logical replication | Required for CDC |
| Replication slot | Debezium tracks database changes through slots |
| Debezium | Captures DB changes and publishes them to Kafka |
| Kafka Connect | Runs Debezium connectors |
| CDC | Change Data Capture replaces application polling |
| Outbox event routing | Outbox rows should become domain-specific topics |
| Connector offset | CDC needs progress tracking |
| Connector failure recovery | Replication can lag or fail |
| Schema changes with CDC | DB migrations affect event extraction |
| Operational ownership | CDC becomes part of platform operations |
| Backfill vs CDC | Existing data and new changes are different problems |

## Practical Implementation Tasks

- Enable PostgreSQL logical replication:

```text
wal_level=logical
```

- Add Debezium and Kafka Connect to Docker Compose.
- Create Debezium connector for `outbox_events`.
- Route events by `eventType`.
- Remove or disable application poller.
- Monitor connector lag.
- Simulate connector outage and recovery.
- Add migration test where outbox schema evolves.
- Compare application polling latency vs Debezium latency.
- Document ownership:
  - who monitors connectors
  - how to restart connectors
  - how to recover from broken schema changes

## Architectural Decision

### Decision

Use Debezium to capture outbox rows from PostgreSQL WAL.

### Why

CDC reduces polling pressure and improves event extraction latency while preserving transactional outbox correctness.

### Alternatives Considered

| Alternative | Why Not Enough |
|---|---|
| Keep polling forever | Can increase DB pressure and latency |
| Publish directly from app | Reintroduces dual-write risk |
| Database triggers | Harder to version and test |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Lower app polling pressure | More infrastructure |
| Faster event extraction | Connector operations needed |
| Preserves outbox correctness | CDC schema changes need care |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| CDC integration tests | Outbox rows become Kafka events |
| Connector failure tests | CDC resumes after outage |
| Migration compatibility tests | Schema changes do not break routing |
| Lag monitoring tests | Connector lag is visible |

Acceptance criteria:

- Debezium publishes outbox events to Kafka topics.
- Application poller is disabled.
- Connector lag is visible in metrics.
- A runbook exists for connector restart and recovery.

## Staff Engineer Lens

- CDC is not only a developer feature; it is operational infrastructure.
- WAL-based extraction improves event publishing but adds connector ownership.
- Migration design must include downstream CDC consumers.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Debezium Outbox Event Router** — Use or evaluate the outbox routing pattern so rows become domain-specific Kafka records cleanly.
- **Snapshot strategy** — Document whether the connector takes an initial snapshot, schema-only snapshot, or no snapshot.
- **Replication slot monitoring** — Track slot lag and disk growth risk when Kafka Connect or Debezium is down.
- **Connector config as code** — Store connector configuration in the repository and make local creation repeatable.
- **CDC migration compatibility** — Every outbox table migration must state how Debezium routing and existing connectors remain compatible.

---
