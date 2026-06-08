# Phase 15 — Observability, SLOs, and Incident Readiness

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 15 of 18

## Business Goal

When payments fail, invoices are delayed, or Kafka lag grows, engineers must quickly understand what happened and what users are affected.

## Technical Goal

Make the platform observable, diagnosable, and ready for production-like incidents.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Structured logs | Logs need machine-readable fields |
| Log sampling | High-volume logs need cost control |
| Metrics | System behavior needs numeric visibility |
| Metric cardinality | Too many labels can break metrics systems |
| Traces | Distributed calls need causal paths |
| Trace sampling | Not every trace can always be retained |
| Span attributes | Traces need useful business and technical context |
| Baggage propagation | Cross-service metadata needs care |
| OpenTelemetry | Vendor-neutral instrumentation |
| Prometheus | Local metrics collection |
| Grafana | Dashboards and alert visualization |
| Loki | Local log aggregation |
| Tempo | Local distributed tracing |
| RED metrics | Request rate, errors, duration |
| USE metrics | Utilization, saturation, errors |
| Business metrics | Payments and invoices need domain-level visibility |
| SLI | What you measure for reliability |
| SLO | Target reliability objective |
| SLA | External promise, learned conceptually |
| Error budget | Reliability target translated into change risk |
| Alert severity | Not all alerts require same response |
| Alert fatigue | Too many noisy alerts destroy trust |
| Runbook | Alerts need actionable steps |
| Incident postmortem | Incidents should improve the system |
| Synthetic monitoring | Important flows can be tested continuously |
| Black-box monitoring | User-visible behavior matters |
| Dashboard ownership | Dashboards need maintainers |

## Practical Implementation Tasks

- Add OpenTelemetry Java agent.
- Propagate trace context across:
  - REST
  - Kafka
  - scheduled jobs
- Add correlation ID to logs and events.
- Add JSON logs.
- Add trace ID and span ID to log output.
- Add business metrics:

```text
invoices.generated.total
payments.authorized.total
payments.succeeded.total
payments.failed.total
payment.workflow.duration
ledger.posting.duration
idempotency.replay.total
kafka.consumer.lag
outbox.events.pending
```

- Add RED dashboards for each service.
- Add USE dashboard for JVM and database resources.
- Add business KPI dashboard.
- Add alert rules:
  - high payment failure rate
  - high Kafka consumer lag
  - outbox backlog growing
  - payment workflow stuck
  - reconciliation mismatch count > 0
- Add runbooks for each alert.
- Add incident postmortem template.
- Add SLOs:

```text
99.5% of payment authorization requests complete successfully within 2 seconds over 30 days.
99.9% of invoice generation requests complete successfully within 1 second over 30 days.
99% of payment events are processed within 60 seconds.
```

## Architectural Decision

### Decision

Use OpenTelemetry with Prometheus, Grafana, Loki, and Tempo locally.

### Why

It gives production-like debugging without vendor lock-in or paid cloud services.

### Alternatives Considered

| Alternative | Why Not Enough |
|---|---|
| Logs only | Hard to understand distributed flows |
| Metrics only | Shows symptoms but not causal paths |
| Traces only | Poor for aggregate alerting |
| Vendor SaaS | Violates local-first constraint |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Better incident diagnosis | More local infrastructure |
| Clear reliability targets | More metric discipline |
| Actionable alerts | Runbooks must be maintained |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Observability smoke tests | Logs, metrics, and traces are emitted |
| Trace propagation tests | Correlation survives service boundaries |
| Alert rule tests | Important failure modes trigger alerts |
| Runbook drill | An engineer can follow steps to diagnose issue |
| Synthetic flow test | Critical business path is externally visible |

Acceptance criteria:

- A failed payment can be traced across API, service, Kafka, and database work.
- Payment failure rate alert has a runbook.
- Kafka lag is visible on a dashboard.
- SLOs and error budgets are documented.

## Staff Engineer Lens

- Observability is part of architecture, not an afterthought.
- An alert without a runbook is often just anxiety automation.
- SLOs help teams decide when to ship and when to stabilize.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Burn-rate alerting** — Add fast-burn and slow-burn alerts for SLOs instead of only static threshold alerts.
- **Trace-log-metric correlation standard** — Every dashboard panel and runbook should show how to jump between metric spike, trace, and logs.
- **Business impact fields** — Expose affected tenant, invoice count, payment count, and estimated money amount where safe.
- **Dashboard review cadence** — Assign owners and review dashboards after incidents or major architecture changes.
- **Synthetic journey coverage** — Add scheduled checks for subscribe → invoice → payment → ledger → notification.

---
