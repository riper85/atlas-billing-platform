# Phase 11 — Kafka Event Streaming

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 11 of 18

## Business Goal

The platform needs scalable event distribution, replay, and multiple independent consumers.

## Technical Goal

Move from local event dispatching to Kafka-based event streams while preserving reliability rules learned earlier.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Kafka topic | Events need named streams |
| Partition | Throughput and ordering are partition-scoped |
| Partition key | Ordering must be designed per business entity |
| Consumer group | Consumers need horizontal scaling |
| Offset | Consumers need progress tracking |
| Offset commit strategy | Processing and commit order define duplicate risk |
| Manual offset commit | Critical consumers need explicit control |
| Rebalancing | Consumer ownership changes at runtime |
| Consumer pause/resume | Backpressure and poison messages need control |
| Producer acknowledgements | Durability depends on broker acknowledgements |
| Producer retries | Transient broker failures should be retried safely |
| Idempotent producer | Prevent duplicate writes during producer retries |
| Kafka transactions | Useful for consume-process-produce flows |
| Exactly-once semantics | Scoped guarantee, not general magic |
| Ordering guarantee | Ordering exists only within a partition |
| Retry topics | Failed events need delayed retry without blocking partitions |
| Dead-letter topic | Bad events need isolation and investigation |
| Event replay | Consumers may rebuild projections |
| Compacted topic | Latest state per key can be retained |
| Tombstone event | Deletion in compacted topics needs explicit markers |
| Schema Registry | Event schemas need governance |
| Avro vs JSON Schema vs Protobuf | Serialization choice affects compatibility and tooling |
| Schema compatibility mode | Producers and consumers must evolve safely |

## Topics

```text
billing.invoice.generated.v1
billing.payment.authorized.v1
billing.payment.succeeded.v1
billing.payment.failed.v1
billing.ledger.posted.v1
billing.subscription.cancelled.v1
billing.customer.snapshot.v1
```

## Partition Key Strategy

| Event Type | Partition Key | Why |
|---|---|---|
| Invoice events | `invoiceId` | Preserve invoice lifecycle order |
| Subscription events | `subscriptionId` | Preserve subscription state order |
| Payment events | `paymentId` | Preserve payment state order |
| Customer snapshot | `customerId` | Keep latest customer state compactable |
| Ledger events | `accountId` | Preserve account posting order |

## Practical Implementation Tasks

- Run Kafka in KRaft mode locally.
- Add Schema Registry.
- Publish outbox events to Kafka.
- Add Kafka producer config:
  - `acks=all`
  - idempotence enabled
  - bounded retries
  - delivery timeout
- Add consumer config:
  - manual commit for critical consumers
  - max poll interval tuning
  - dead-letter publishing
- Add retry topics:

```text
billing.payment.failed.retry.1m
billing.payment.failed.retry.10m
billing.payment.failed.dlt
```

- Add consumer lag metrics.
- Add event replay exercise for reporting projection.
- Add compacted topic for customer snapshot.
- Add tombstone event for removed snapshot.
- Compare Avro, JSON Schema, and Protobuf for event contracts.
- Configure schema compatibility mode.

## Architectural Decision

### Decision

Publish outbox events to Kafka with explicit partition keys and schema governance.

### Why

Consumers can scale independently, but ordering and compatibility must be designed intentionally.

### Alternatives Considered

| Alternative | Why Not Chosen |
|---|---|
| RabbitMQ | Good broker, but less focused on replayable event streams |
| Redis Streams | Useful, but Kafka is more common for event-streaming architecture |
| Direct REST callbacks | Tight coupling and complex retry behavior |
| Random partitioning | Higher throughput but loses entity ordering |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Scalable event distribution | More operational complexity |
| Replayable events | Consumers must handle old events |
| Independent consumers | Schema governance becomes necessary |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Kafka integration tests | Producers and consumers work locally |
| Duplicate delivery tests | Consumers remain idempotent |
| Offset commit tests | Failures do not lose events |
| Replay tests | Projection can rebuild from events |
| Schema compatibility tests | Event changes do not break consumers |

Acceptance criteria:

- Events are published from outbox to Kafka.
- Consumers commit offsets only after successful processing.
- Retry topics and DLT work.
- Reporting projection can be rebuilt from Kafka events.
- Partition key choices are documented in an ADR.

## Staff Engineer Lens

- Kafka is not async magic. It is a distributed log with operational rules.
- Ordering is scoped, not global.
- Exactly-once semantics does not remove the need for idempotent business logic.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Partition-key verification tests** — Add tests or fixtures that prove event keys use the documented aggregate ID and preserve expected ordering.
- **Consumer rebalance strategy** — Document cooperative vs eager rebalancing choice and test shutdown/restart behavior.
- **Producer batching trade-offs** — Tune linger, batch size, compression, and delivery timeout intentionally instead of using defaults blindly.
- **Schema compatibility workflow** — Add a local compatibility check before registering or publishing changed schemas.
- **Topic ownership metadata** — For each topic, record owner, retention, compaction, partition count, DLT, and expected consumers.

---
