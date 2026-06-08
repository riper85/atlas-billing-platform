# Phase 14 Extra — Real Payment Provider Integration Behind Payment Service

> **Attach to:** `phase-14-service-extraction-readiness-and-payment-boundary-validation.md`  
> **Purpose:** Make the “real payment provider integration” scope explicit and implementable after ports, idempotency, workflows, Kafka, and service extraction readiness exist.  
> **Architecture stance:** `payment-service` is still an extraction candidate, not proof that every module must become a microservice.

---

## Why This Belongs in Phase 14

Earlier phases prepare the ground:

- Phase 04 creates payment provider ports and a stub adapter.
- Phase 06 adds API reliability, idempotency, and webhook replay protection.
- Phase 10 introduces outbox/inbox for reliable event publication and consumption.
- Phase 11 introduces Kafka event streaming.
- Phase 13 models distributed workflows and reconciliation.
- Phase 14 validates whether payment deserves extraction because it has external provider integration, retry complexity, failure isolation needs, and a different change rate.

This extra makes the provider integration practical and concrete.

---

## Still Out of Scope Here

| Topic | Why Not Here | Later Phase |
|---|---|---|
| Multiple real providers with smart routing | Start with one provider and one local fallback. | Later payment maturity phase |
| PCI-grade card storage | Do not store raw card data locally. | Requires specialized compliance scope |
| Full fraud platform | Fraud is a separate domain. | Later workflow/risk phase |
| Full accounting settlement | Provider settlement needs deeper finance flows. | Later ledger/reconciliation expansion |
| Global production deployment | This remains local-first. | Delivery/platform phases |

---

## Provider Integration Maturity Split

| Phase | Payment Provider Scope |
|---|---|
| Phase 04 | Define `PaymentProviderPort` and stub adapter only. |
| Phase 06 | Add idempotency, timeout, retry budget, and webhook safety concepts. |
| Phase 13 | Coordinate payment workflow, compensation, timeout, recovery, and reconciliation. |
| Phase 14 | Move provider integration behind `payment-service` and validate extraction pressure. |

---

## Extra Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Provider adapter | External API details must stay outside core payment model. |
| Provider idempotency key | Provider retries must not double-charge. |
| Provider status mapping | External statuses differ from internal payment states. |
| Webhook endpoint | Providers report async payment results. |
| Webhook signature verification | Provider callbacks must be authenticated. |
| Webhook replay protection | Old callbacks must not be reused maliciously. |
| Provider reconciliation | Provider truth and internal truth can diverge. |
| Shadow provider mode | New integration can run without affecting visible state. |
| Fallback routing | Payment path must be reversible during migration. |

---

## Workable Chunk 14.E1 — Payment Service Provider Boundary

### Goal

Keep provider details inside `payment-service`.

### Implement

```text
services/payment-service/src/main/java/com/atlas/payment/
  payment/
    api/
    application/
    domain/
    port/
      in/
      out/
    adapter/
      in/web/
      out/provider/
      out/postgres/
      out/kafka/
```

### Public Commands

```http
POST /api/v1/payments/authorize
POST /api/v1/payments/{paymentId}/capture
POST /api/v1/payments/{paymentId}/refund
```

### Provider Port

```text
PaymentProviderPort
  authorize(ProviderAuthorizeRequest request)
  capture(ProviderCaptureRequest request)
  refund(ProviderRefundRequest request)
```

### Acceptance Gate

- Billing platform does not call provider directly.
- Provider DTOs do not leak outside `payment-service` adapter layer.
- Provider adapter can be replaced by WireMock/local adapter.

---

## Workable Chunk 14.E2 — Provider Adapter with WireMock

### Goal

Implement realistic provider behavior locally without using a paid provider.

### Simulate Provider Responses

```text
201 authorized
202 pending
400 invalid request
401 invalid credentials
402 payment declined
409 duplicate provider idempotency key
429 rate limited
500 provider failure
timeout
```

### Implement

```text
WireMockPaymentProviderAdapter
ProviderPaymentMapper
ProviderErrorMapper
```

### Acceptance Gate

- Successful authorization maps to internal `AUTHORIZED` state.
- Decline maps to business payment failure, not technical retry.
- 429/500/timeout map to retryable technical failure.
- Provider-specific error body is not exposed directly to billing clients.

---

## Workable Chunk 14.E3 — Provider Idempotency

### Goal

Propagate internal idempotency safely to the provider boundary.

### Rules

- Internal payment command has an `Idempotency-Key`.
- Payment service stores its own idempotency record.
- Provider request uses a provider idempotency key derived from internal payment command identity.
- Same provider key must not be reused for a different amount/currency/payment action.

### Suggested Provider Key Shape

```text
paymentServiceOperation + paymentId + commandId
```

Example:

```text
authorize:pay_123:cmd_456
capture:pay_123:cmd_789
refund:pay_123:cmd_999
```

### Acceptance Gate

- Retried authorization does not create duplicate provider charge/authorization.
- Same idempotency key with different payload is rejected.
- Provider duplicate response maps to original internal result where possible.

---

## Workable Chunk 14.E4 — Payment State Mapping

### Goal

Protect internal state machine from provider-specific statuses.

### Example Mapping

| Provider Status | Internal State | Notes |
|---|---|---|
| `requires_action` | `PENDING_CUSTOMER_ACTION` | May require redirect/3DS later. |
| `authorized` | `AUTHORIZED` | Funds reserved. |
| `captured` | `CAPTURED` | Money captured. |
| `declined` | `FAILED` | Business failure. |
| `expired` | `EXPIRED` | Authorization expired. |
| `refunded` | `REFUNDED` | Refund complete. |
| unknown | `PROVIDER_UNKNOWN` or technical mapping error | Must not silently succeed. |

### Acceptance Gate

- Unknown provider status is handled safely.
- Invalid internal transition is rejected.
- Mapping has exhaustive tests.
- Internal state names remain provider-neutral.

---

## Workable Chunk 14.E5 — Webhook Endpoint

### Goal

Accept async provider events safely.

### Endpoint

```http
POST /api/v1/provider-webhooks/payment-provider
```

### Implement

```text
PaymentProviderWebhookController
WebhookSignatureVerifier
WebhookReplayProtectionService
ProviderWebhookMapper
RegisterProviderPaymentEventUseCase
```

### Validate

- raw body signature
- timestamp tolerance
- provider event ID uniqueness
- provider account/client ID where available
- supported event type

### Acceptance Gate

- Valid webhook updates payment state.
- Invalid signature is rejected.
- Replayed webhook event is ignored or returns safe duplicate response.
- Out-of-order webhook is handled by state transition rules.

---

## Workable Chunk 14.E6 — Payment Events from Payment Service

### Goal

Publish payment facts for billing, ledger, notification, and reconciliation.

### Events

```text
billing.payment.authorized.v1
billing.payment.authorization_failed.v1
billing.payment.captured.v1
billing.payment.capture_failed.v1
billing.payment.refunded.v1
billing.payment.refund_failed.v1
```

### Rules

- Events are written through payment-service outbox.
- Events include correlation ID and causation ID.
- Events use stable payment IDs, not provider-only IDs.
- Provider IDs may appear as internal metadata only if safe.

### Acceptance Gate

- Payment state change and outbox event commit atomically.
- Kafka publication happens from outbox, not directly inside business transaction.
- Duplicate publication does not duplicate downstream effects.

---

## Workable Chunk 14.E7 — Billing Platform Integration

### Goal

Route billing payment commands to either monolith payment module or extracted payment-service.

### Feature Flag

```text
payment.routing=modular-monolith | payment-service | shadow
```

### Modes

| Mode | Behavior |
|---|---|
| `modular-monolith` | Existing in-process payment path remains source of truth. |
| `payment-service` | Billing calls extracted payment-service. |
| `shadow` | Billing uses monolith result but also sends shadow request to payment-service for comparison. |

### Acceptance Gate

- Routing can switch without code changes.
- Shadow mode does not affect user-visible state.
- Differences between monolith and service results are logged/recorded.
- Rollback to monolith path is documented.

---

## Workable Chunk 14.E8 — Provider Reconciliation

### Goal

Detect divergence between provider state and internal payment state.

### Implement

```text
ProviderPaymentLookupPort
PaymentProviderReconciliationJob
PaymentReconciliationIssue
```

### Checks

```text
- internal AUTHORIZED but provider says expired
- internal CAPTURED but provider has no capture
- provider captured but internal payment not captured
- refund exists at provider but not internally
```

### Acceptance Gate

- Reconciliation can query provider stub/WireMock.
- Divergence creates manual review issue.
- Reconciliation does not mutate financial state blindly.
- Manual repair path is documented.

---

## Workable Chunk 14.E9 — Resilience Policies

### Goal

Contain provider failures.

### Add Resilience4j Policies

```text
providerAuthorizeTimeout
providerCaptureTimeout
providerRefundTimeout
providerCircuitBreaker
providerBulkhead
providerRetryWithJitter
providerRateLimiter
```

### Rules

- Retry only safe, idempotent provider calls.
- Do not retry business declines.
- Circuit breaker opening should return a clear temporary failure.
- Bulkhead prevents provider slowness from exhausting all app threads.

### Acceptance Gate

- Timeout test fails predictably.
- 500/timeout uses retry with jitter.
- Decline is not retried.
- Circuit breaker opens after repeated provider failures.
- Bulkhead protects unrelated payment operations.

---

## Testing Additions

| Test Type | What It Proves |
|---|---|
| Pact tests | Billing platform and payment-service contract is stable. |
| WireMock provider tests | Provider success/failure cases are mapped safely. |
| Idempotency tests | Retries do not double-authorize/capture/refund. |
| Webhook security tests | Invalid/replayed webhooks are rejected. |
| State transition tests | Provider events cannot create illegal payment states. |
| Outbox tests | Payment events are published reliably. |
| Shadow comparison tests | Service path can be compared before cutover. |
| Reconciliation tests | Provider/internal divergence is detected. |
| Resilience tests | Timeout/retry/circuit breaker/bulkhead policies work. |

---

## Acceptance Criteria for This Extra

- `payment-service` owns provider integration and its own database.
- Billing platform no longer needs provider-specific code.
- Provider adapter is tested against WireMock scenarios.
- Provider idempotency is implemented and tested.
- Webhook endpoint verifies signature and prevents replay.
- Payment-service publishes payment events through outbox/Kafka.
- Feature flag supports monolith, service, and shadow routing.
- Provider reconciliation detects mismatches without unsafe automatic mutation.
- Rollback path to modular-monolith payment behavior exists.

---

## Staff Engineer Lens

- The payment provider is not just an HTTP API; it is an unreliable external system with money-moving side effects.
- Provider integration must be isolated, idempotent, observable, and reversible.
- Extraction is justified only when the provider boundary creates enough change, failure, and ownership pressure.
