# Phase 6 — API Reliability and Idempotency

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 06 of 18

## Business Goal

Clients retry payment and invoice requests after timeouts. The system must not double-charge, double-generate invoices, or return inconsistent responses.

## Technical Goal

Make external APIs safe under retries, network failures, duplicate requests, and client misuse.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Idempotency key | Retried commands must not duplicate side effects |
| Idempotency key scope | Keys must be scoped by tenant, endpoint, and operation |
| Idempotency key expiry | Storage cannot grow forever |
| Request hash | Same key with different payload must be rejected |
| Response replay | Duplicate identical requests should return the original result |
| Unique constraint | Duplicate creation must be prevented atomically |
| Replay protection | Old keys should not be abused indefinitely |
| Correlation ID | Requests must be traceable across logs and events |
| Causation ID | Follow-up work should point to the triggering command |
| Rate limiting | Protect APIs from abusive or accidental traffic |
| Throttling | Slow clients down instead of hard failing everything |
| Timeout budget | Requests need clear maximum time boundaries |
| Deadline propagation | Downstream calls should know how much time remains |
| Retry budget | Retries must not amplify incidents |
| Exponential backoff | Retry pressure should reduce over time |
| Jitter | Avoid synchronized retry storms |
| Retry storm prevention | Reliability features can cause outages if uncontrolled |
| Bulk API design | Some operations should be batched intentionally |
| Webhook signature verification | Provider callbacks must be authenticated |
| Webhook replay protection | Old provider events should not be replayed maliciously |

## Idempotency Storage Model

```text
idempotency_records
  id
  tenant_id
  operation
  idempotency_key
  request_hash
  status: IN_PROGRESS | COMPLETED | FAILED
  http_status
  response_body
  locked_until
  expires_at
  created_at
```

## Practical Implementation Tasks

- Require `Idempotency-Key` for:
  - payment authorization
  - payment capture
  - invoice generation
  - subscription cancellation
- Scope the key by:

```text
tenantId + operation + idempotencyKey
```

- Store request hash and compare duplicates.
- Return the same response for duplicate identical requests.
- Reject same key with different payload using `409 Conflict`.
- Add expiry and cleanup job for old idempotency records.
- Handle `IN_PROGRESS` duplicate requests safely.
- Add correlation ID filter.
- Add causation ID to commands and events.
- Add rate limiting at the API layer using Bucket4j or Resilience4j RateLimiter.
- Add throttling for noisy clients.
- Add timeout configuration per endpoint type.
- Add retry policy with exponential backoff and jitter for safe outbound calls.
- Add webhook endpoint for payment provider events.
- Verify webhook signatures.
- Reject webhook replay attempts.

## Architectural Decision

### Decision

Require idempotency keys for side-effecting financial commands.

### Why

Network retries are normal. Duplicate financial side effects are unacceptable.

### Alternatives Considered

| Alternative | Why Not Enough |
|---|---|
| Disable button in frontend | Does not solve network retries or mobile reconnects |
| Database uniqueness only | Prevents duplicates but does not replay original response |
| Ignore duplicate requests | Financially unsafe |
| Make every endpoint idempotent by natural key only | Not always possible for commands |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Safe retries | Extra storage |
| Better client experience | More edge cases |
| Prevents duplicate charges | Requires strict API rules |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Idempotency replay tests | Duplicate request returns original response |
| Payload mismatch tests | Same key with different request is rejected |
| Concurrent duplicate tests | Race conditions do not duplicate side effects |
| Rate-limit tests | Noisy clients are controlled |
| Webhook replay tests | Old or duplicated callbacks are rejected |
| Timeout tests | Requests fail predictably |

Acceptance criteria:

- Retried payment requests do not double-charge.
- Duplicate invoice generation returns the first response.
- Same idempotency key with a different payload returns a clear conflict.
- Rate-limited clients receive a standard error response.

## Staff Engineer Lens

- Idempotency is not one annotation. It is API design, storage, concurrency, and operations.
- Retries without budgets and jitter can make incidents worse.
- Correlation and causation IDs make reliability debuggable.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Idempotency state machine** — Document transitions between `IN_PROGRESS`, `COMPLETED`, `FAILED`, `EXPIRED`, and stuck/lock-recovery states.
- **Canonical request hashing** — Define exactly which headers, path fields, query params, and body fields are included in the request hash.
- **Safe retry taxonomy** — Classify which operations may be retried automatically and which require manual decision.
- **Single-flight protection** — Prevent multiple threads from executing the same idempotency key concurrently.
- **Webhook canonical signature input** — Define how raw body, timestamp, provider ID, and secret are combined for signature verification.

---
