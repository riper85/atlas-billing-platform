# Phase 10 Extra — Reliable Notification Delivery Foundation

> **Attach to:** `phase-10-outbox-inbox-and-event-driven-architecture.md`  
> **Purpose:** Turn the existing notification consumer idea into an implementable, reliable notification delivery foundation before Kafka.  
> **Architecture stance:** Still a modular monolith. Notification is an internal consumer of outbox-dispatched events.

---

## Why This Belongs in Phase 10

Phase 10 introduces outbox, inbox, event envelopes, idempotent event handlers, retry/dead-letter storage, recovery jobs, and notification as a consumer. Notification delivery belongs here at the reliability-pattern level because it is one of the first real consumers that must handle duplicates, retries, poison messages, and reconciliation.

This phase should not depend on Kafka yet. Kafka-backed scaling comes in Phase 11.

---

## Still Out of Scope Here

| Topic | Why Not Here | Later Phase |
|---|---|---|
| Kafka notification topics | Phase 10 teaches outbox/inbox before Kafka. | Phase 11 |
| Real external email/SMS provider | First build reliability mechanics with a local adapter. | Phase 11 or Phase 14+ depending on extraction |
| Notification microservice extraction | Boundary must be validated first. | Phase 14 or later |
| Full template localization | Useful, but too much product detail here. | Later notification/product phase |
| Marketing campaigns | Not part of billing reliability. | Separate product capability later |

---

## Extra Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Notification command | Event handlers should create explicit delivery intent. |
| Notification template | Delivery content should not be hardcoded inside handlers. |
| Delivery channel | Email, SMS, and webhook delivery differ. |
| Delivery attempt | Retries need attempt history. |
| Provider response mapping | External/local provider details should not leak into domain decisions. |
| Idempotent delivery | Duplicate events must not send duplicate notifications. |
| Dead-lettered notification | Permanently failed notifications need investigation. |
| Notification reconciliation | Expected notifications should match actual delivery state. |

---

## Workable Chunk 10.E1 — Notification Module Structure

### Goal

Create a real notification module boundary without extracting a service.

### Implement

```text
notification/
  api/
  application/
  domain/
  infrastructure/
    template/
    delivery/
    persistence/
```

### Public API

```text
NotificationCommandApi
  requestNotification(NotificationCommand command)
```

### Guardrail

Other modules should not write notification tables directly. They should emit events or call the public notification API.

### Acceptance Gate

- Notification internals are not accessed by invoice/payment modules directly.
- Notification module has its own application service.
- Notification module has architecture tests or Spring Modulith verification.

---

## Workable Chunk 10.E2 — Notification Domain Model

### Goal

Represent notification delivery as stateful business work.

### Implement

```text
NotificationRequest
  notificationId
  sourceEventId
  recipient
  channel
  templateKey
  templateData
  status
  correlationId
  causationId
  createdAt

NotificationDeliveryAttempt
  attemptId
  notificationId
  attemptNumber
  status
  providerMessageId
  errorCode
  errorMessage
  attemptedAt
```

### Statuses

```text
REQUESTED
READY_TO_SEND
SENDING
SENT
FAILED_RETRYABLE
FAILED_PERMANENT
DEAD_LETTERED
CANCELLED
```

### Acceptance Gate

- Duplicate source event does not create duplicate `NotificationRequest` records.
- Delivery attempts are append-only.
- Status transitions are tested.

---

## Workable Chunk 10.E3 — Event-to-Notification Mapping

### Goal

Map integration events to notification requests without coupling delivery logic to event payloads everywhere.

### Example Mappings

| Event | Template | Recipient Source |
|---|---|---|
| `InvoiceGenerated` | `invoice-generated-email` | customer billing email |
| `PaymentSucceeded` | `payment-succeeded-email` | customer billing email |
| `PaymentFailed` | `payment-failed-email` | customer billing email |
| `SubscriptionCancelled` | `subscription-cancelled-email` | customer billing email |

### Implement

```text
NotificationPolicy
NotificationRecipientResolver
NotificationTemplateSelector
```

### Acceptance Gate

- Each supported event maps to a deterministic template.
- Missing recipient creates a retryable or permanent failure based on the cause.
- Unsupported event is ignored explicitly, not accidentally.

---

## Workable Chunk 10.E4 — Local Template Rendering

### Goal

Introduce templates without adding a real external email provider.

### Implement

```text
TemplateRenderer
TemplateKey
TemplateData
RenderedNotification
```

Suggested local templates:

```text
invoice-generated-email
payment-succeeded-email
payment-failed-email
subscription-cancelled-email
```

### Rules

- Template rendering must fail if required variables are missing.
- Rendered content should include invoice/payment/subscription IDs where appropriate.
- Templates should not contain provider-specific concepts.

### Acceptance Gate

- Template rendering is unit-tested.
- Missing template variable causes a controlled failure.
- Rendered content can be inspected locally.

---

## Workable Chunk 10.E5 — Local Delivery Adapter

### Goal

Practice delivery flow without depending on SendGrid, SES, Twilio, or another external system.

### Implement

```text
NotificationDeliveryPort
LocalEmailDeliveryAdapter
LocalSmsDeliveryAdapter optional
```

Local behavior options:

```text
- write delivery to database table
- write delivery to local file
- expose local dev inbox endpoint
- log structured delivery event
```

### Acceptance Gate

- Sending an invoice notification creates a delivery attempt.
- Local inbox shows sent messages.
- Provider-specific response is mapped into internal delivery result.

---

## Workable Chunk 10.E6 — Idempotent Delivery

### Goal

Guarantee duplicate events do not cause duplicate sends.

### Implement

Unique constraint:

```text
unique(source_event_id, template_key, recipient, channel)
```

Or explicit business key:

```text
notification_deduplication_key = eventId + templateKey + recipient + channel
```

### Rules

- Duplicate event replays should return existing notification result.
- Duplicate send attempt after crash should not create two successful sends for the same notification.
- Delivery adapter should be called only when state transition allows it.

### Acceptance Gate

- Duplicate event handler execution creates one notification.
- Concurrent duplicate handling creates one notification.
- Existing `SENT` notification is not sent again.

---

## Workable Chunk 10.E7 — Retry and Dead-Letter Handling

### Goal

Use Phase 10 retry/dead-letter mechanics for notification delivery.

### Add Fields

```text
next_attempt_at
attempt_count
max_attempts
last_error_code
last_error_message
```

### Failure Classification

| Failure | Classification |
|---|---|
| provider timeout | retryable |
| connection refused | retryable |
| invalid recipient address | permanent |
| missing template | permanent |
| malformed template data | permanent |

### Acceptance Gate

- Retryable failure schedules a later attempt.
- Permanent failure moves to `FAILED_PERMANENT` or `DEAD_LETTERED`.
- Retry attempts stop after configured maximum.
- Dead-lettered notification has enough investigation data.

---

## Workable Chunk 10.E8 — Notification Reconciliation

### Goal

Prove reliable systems can detect missing or inconsistent async side effects.

### Implement Reconciliation Checks

```text
- issued invoice should have invoice notification request
- successful payment should have payment success notification request
- failed payment should have payment failure notification request
- sent notification should have at least one successful delivery attempt
```

### Output

```text
NotificationReconciliationIssue
  issueId
  sourceAggregateId
  expectedNotificationType
  actualState
  severity
  createdAt
```

### Acceptance Gate

- Intentionally deleted notification request is detected.
- Missing successful delivery attempt is detected.
- Reconciliation issues are queryable.

---

## Testing Additions

| Test Type | What It Proves |
|---|---|
| Inbox deduplication tests | Duplicate event does not duplicate notification. |
| Template tests | Required variables and rendering rules work. |
| Delivery adapter tests | Provider/local delivery results map correctly. |
| Retry tests | Retryable failures are retried safely. |
| Dead-letter tests | Permanent failures are isolated. |
| Reconciliation tests | Missing notifications are detected. |

---

## Acceptance Criteria for This Extra

- Notification module owns notification request and delivery attempt records.
- Invoice/payment/subscription events can create notification requests.
- Local template rendering works.
- Local delivery adapter records sent messages.
- Duplicate events do not send duplicate notifications.
- Retry and dead-letter handling are implemented with database-backed mechanics.
- Reconciliation detects missing notification side effects.
- No Kafka or external provider is required in this phase.

---

## Staff Engineer Lens

- Notification delivery looks simple until retries, duplicates, and provider failures appear.
- A sent email/SMS is a side effect and must be treated as such.
- Reliable async work requires deduplication, retry policy, investigation data, and reconciliation.
