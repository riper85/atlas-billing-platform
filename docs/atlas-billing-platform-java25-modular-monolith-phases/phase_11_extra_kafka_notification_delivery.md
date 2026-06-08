# Phase 11 Extra — Kafka-Backed Notification Delivery

> **Attach to:** `phase-11-kafka-event-streaming.md`  
> **Purpose:** Extend notification delivery from Phase 10 database-backed outbox/inbox mechanics to Kafka-backed event streaming.  
> **Architecture stance:** Still no big-bang microservices. Kafka is introduced for scalable event distribution, replay, and independent consumers.

---

## Why This Belongs in Phase 11

Phase 11 introduces Kafka topics, partitions, partition keys, consumer groups, offset commits, retry topics, dead-letter topics, event replay, schema registry, and compatibility. Notification delivery is a strong Kafka use case because it is asynchronous, can tolerate eventual consistency, and benefits from retry/DLT/replay mechanics.

Phase 10 proves reliable delivery mechanics locally. Phase 11 moves the event transport to Kafka.

---

## Still Out of Scope Here

| Topic | Why Not Here | Later Phase |
|---|---|---|
| Notification service extraction | Kafka can be used inside the modular system first. | Phase 14 or later |
| Full marketing platform | Billing notifications are operational, not campaign management. | Separate product phase |
| Multi-provider routing engine | Start with one primary provider and one fallback/local adapter. | Later notification maturity phase |
| Full localization workflow | Useful later, not required for Kafka delivery mechanics. | Later product/content phase |
| Exactly-once business guarantee | Kafka EOS is scoped; consumers still need idempotency. | Always keep idempotent consumers |

---

## Extra Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Notification event topic | Notification needs a dedicated stream of delivery requests. |
| Notification partition key | Ordering should be scoped per notification/customer. |
| Consumer group scaling | Multiple notification workers can process in parallel. |
| Manual offset commit | Commit only after safe persistence/handler success. |
| Retry topic per delay | Failed sends should not block the main topic. |
| Dead-letter topic | Poison notification messages need investigation. |
| Replayable notifications | Rebuild delivery state or reprocess failed messages safely. |
| Schema compatibility | Notification event payloads evolve over time. |

---

## Workable Chunk 11.E1 — Notification Topics

### Goal

Define notification-specific Kafka topics with ownership and retention.

### Add Topics

```text
billing.notification.requested.v1
billing.notification.sent.v1
billing.notification.failed.v1
billing.notification.retry.1m.v1
billing.notification.retry.10m.v1
billing.notification.dlt.v1
```

### Topic Ownership Metadata

| Topic | Owner | Retention | Compacted | Purpose |
|---|---|---:|---:|---|
| `billing.notification.requested.v1` | notification | 7 days | no | Delivery requests |
| `billing.notification.sent.v1` | notification | 30 days | no | Delivery success facts |
| `billing.notification.failed.v1` | notification | 30 days | no | Delivery failure facts |
| `billing.notification.dlt.v1` | notification/platform | 30+ days | no | Poison/investigation messages |

### Acceptance Gate

- Topics are documented in the event catalog.
- Topic names follow the project naming convention.
- Ownership, retention, and expected consumers are stated.

---

## Workable Chunk 11.E2 — Notification Event Contracts

### Goal

Create stable event contracts for notification delivery.

### Event Envelope

Reuse the project event envelope from Phase 10:

```text
eventId
eventType
eventVersion
tenantId
aggregateType
aggregateId
correlationId
causationId
occurredAt
payload
```

### Payload Examples

```json
{
  "notificationId": "notif-123",
  "recipient": "customer@example.com",
  "channel": "EMAIL",
  "templateKey": "invoice-generated-email",
  "templateData": {
    "invoiceId": "inv-123",
    "amount": "100.00",
    "currency": "EUR"
  }
}
```

### Acceptance Gate

- Schema exists for requested/sent/failed events.
- Compatibility mode is configured.
- Adding an optional field passes compatibility checks.
- Removing or renaming required fields fails compatibility checks.

---

## Workable Chunk 11.E3 — Partition Key Strategy

### Goal

Make notification ordering explicit.

### Strategy Options

| Candidate Key | When to Use | Trade-off |
|---|---|---|
| `notificationId` | Preserve one notification lifecycle order. | No ordering across customer notifications. |
| `customerId` | Preserve customer notification order. | Hot customers can become hot partitions. |
| `tenantId` | Preserve tenant-level order. | Poor parallelism for large tenants. |

### Recommendation

Use `notificationId` for lifecycle events and `customerId` only if customer-level ordering becomes a business requirement.

### Acceptance Gate

- ADR documents chosen key.
- Tests verify producer uses documented key.
- Replay preserves expected notification lifecycle order.

---

## Workable Chunk 11.E4 — Kafka Consumer Handler

### Goal

Consume notification requests safely.

### Implement

```text
NotificationRequestedConsumer
NotificationKafkaMapper
NotificationDeliveryUseCase
```

### Consumer Rules

- Deserialize and validate event envelope.
- Persist inbox/deduplication record before side effects.
- Call notification delivery use case.
- Commit offset only after successful handling or safe dead-letter routing.
- Never send notification before deduplication decision.

### Acceptance Gate

- Duplicate Kafka delivery does not duplicate notification send.
- Consumer commits offset after successful processing.
- Consumer does not commit offset before handler persistence.

---

## Workable Chunk 11.E5 — Retry Topics and DLT

### Goal

Use Kafka retry topics for delayed retry and DLT for poison messages.

### Retry Flow

```text
billing.notification.requested.v1
  -> retryable failure
billing.notification.retry.1m.v1
  -> retryable failure
billing.notification.retry.10m.v1
  -> still failing
billing.notification.dlt.v1
```

### Failure Classification

| Failure | Handling |
|---|---|
| provider timeout | retry topic |
| provider 5xx | retry topic |
| invalid recipient | DLT/permanent failure |
| invalid schema | DLT |
| missing required template data | DLT |

### Acceptance Gate

- Retryable failure is published to retry topic.
- Permanent failure is published to DLT.
- DLT event contains original event, error category, stack/error summary, and correlation ID.
- Retry count is capped.

---

## Workable Chunk 11.E6 — Notification Provider Adapter

### Goal

Move from local delivery to a realistic provider adapter while still runnable locally.

### Implement

```text
EmailProviderPort
LocalEmailProviderAdapter
WireMockEmailProviderAdapter
```

### Local Provider Behavior

Use WireMock to simulate:

```text
202 Accepted
400 invalid recipient
401 invalid credentials
429 rate limited
500 provider failure
timeout
```

### Acceptance Gate

- Provider response mapping is tested.
- 429 and 5xx are retryable.
- 400 invalid recipient is permanent.
- Provider timeout does not block the consumer indefinitely.

---

## Workable Chunk 11.E7 — Replay Procedure

### Goal

Practice replay without accidentally resending already-sent messages.

### Replay Rule

Replay should rebuild or repair notification state, but must not resend messages already marked `SENT` unless an explicit manual repair command allows it.

### Implement

```text
NotificationReplayMode
  REBUILD_STATE_ONLY
  RESEND_FAILED_ONLY
  MANUAL_RESEND
```

### Acceptance Gate

- Replaying old `requested` events does not duplicate already-sent notifications.
- Failed notifications can be replayed safely.
- Manual resend requires explicit command/flag.

---

## Workable Chunk 11.E8 — Metrics

### Goal

Make notification streaming behavior visible.

### Add Metrics

```text
notification.kafka.consumed.total
notification.delivery.sent.total
notification.delivery.failed.total
notification.delivery.retry.total
notification.delivery.dlt.total
notification.consumer.lag
notification.provider.duration
```

### Acceptance Gate

- Consumer lag is visible.
- Retry and DLT counts are visible.
- Provider latency is measured.
- Metrics include low-cardinality labels only.

---

## Testing Additions

| Test Type | What It Proves |
|---|---|
| Kafka integration tests | Notification request events are consumed. |
| Duplicate delivery tests | Consumer remains idempotent. |
| Offset commit tests | Failed processing does not lose messages. |
| Retry/DLT tests | Failures route correctly. |
| Schema compatibility tests | Event contracts evolve safely. |
| Replay tests | Replay does not duplicate sent notifications. |

---

## Acceptance Criteria for This Extra

- Notification Kafka topics are defined and documented.
- Notification event schemas are versioned and compatibility-checked.
- Consumer processes notification requests idempotently.
- Retry topics and DLT work for notification failures.
- Provider failures are simulated with WireMock or local stub.
- Replay does not duplicate already-sent notifications.
- Consumer lag and delivery metrics are visible.

---

## Staff Engineer Lens

- Kafka gives scalable delivery, not correctness by itself.
- Offset commits, idempotency, and DLT design decide whether failures are safe.
- Replay is powerful only when side effects are protected from duplication.
