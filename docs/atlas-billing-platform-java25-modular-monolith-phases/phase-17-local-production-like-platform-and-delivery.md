# Phase 17 — Local Production-like Platform and Delivery

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 17 of 18

## Business Goal

The company wants confidence before production deployment and needs safe ways to release changes.

## Technical Goal

Run locally like a small production system and practice release strategies without paid cloud dependencies.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Docker Compose profiles | Local machine cannot run everything all the time |
| Health checks | Containers need self-reporting |
| Readiness | Service should receive traffic only when ready |
| Liveness | Stuck services need restart signals |
| Graceful shutdown | In-flight requests and Kafka processing need safe stop |
| Resource limits | Local platform should behave predictably |
| Container-aware JVM | JVM must respect container memory and CPU |
| Configuration management | Runtime behavior should not be hardcoded |
| Secret management | Credentials should not live in source code |
| Environment parity | Dev, test, and prod-like configs should differ intentionally |
| Configuration drift | Local and CI configs should not silently diverge |
| Feature flags | Release behavior can be decoupled from deployment |
| Dark launch | New code can run without affecting users |
| Canary release | Small traffic percentage validates change |
| Blue-green deployment | Learn zero-downtime release concept locally |
| Rollback strategy | Code rollback and data rollback differ |
| Release notes | Changes need communication |
| Semantic versioning | Libraries and APIs need version meaning |
| Container image hardening | Images should run with minimal risk |
| kind Kubernetes | Optional step after Compose concepts are understood |

## Docker Compose Profiles

```text
minimal:
  postgres
  billing-app

dev:
  postgres
  redis
  keycloak
  kafka
  schema-registry

messaging:
  kafka
  schema-registry
  kafka-connect
  debezium

observability:
  prometheus
  grafana
  loki
  tempo
  otel-collector

full:
  everything
```

## Practical Implementation Tasks

- Add health endpoints:

```http
/actuator/health/liveness
/actuator/health/readiness
```

- Add readiness checks for:
  - database
  - Kafka
  - Redis
  - Keycloak JWKS
- Add graceful shutdown for:
  - HTTP server
  - Kafka consumers
  - scheduled jobs
- Add JVM container flags appropriate for local development.
- Add resource limits in Docker Compose.
- Add `.env.example` and keep real `.env` out of Git.
- Add local secret rotation exercise.
- Add feature flags for:
  - payment-service routing
  - new invoice calculation
  - new notification provider
- Add dark launch for payment-service.
- Add simple local canary using Compose or gateway routing.
- Add release notes template.
- Add rollback playbook.
- Add optional kind deployment after Docker Compose works.

## Architectural Decision

### Decision

Use Docker Compose first, then optionally kind Kubernetes.

### Why

Docker Compose is enough until orchestration itself becomes the thing being learned.

### Alternatives Considered

| Alternative | Why Not Yet |
|---|---|
| Kubernetes from day one | Too much platform complexity too early |
| Cloud platform | Violates local-first constraint |
| Manual local installs | Inconsistent developer machines |
| Service mesh | Too heavy for this learning system |
| Full GitOps | Useful later, but distracting before platform maturity |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Easy local execution | Less production parity than Kubernetes |
| Profile-based resource control | Compose files need maintenance |
| Optional orchestration learning | More manifests later |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Compose smoke tests | Profiles start correctly |
| Health check tests | Readiness and liveness reflect real state |
| Shutdown tests | In-flight work completes or stops safely |
| Feature flag tests | Runtime behavior can change safely |
| Rollback drill | Previous version can be restored |

Acceptance criteria:

- `make up PROFILE=minimal` starts basic app.
- `make up PROFILE=full` starts full local platform on a 32GB laptop with realistic limits.
- Services expose readiness and liveness endpoints.
- Graceful shutdown does not lose Kafka messages.
- A rollback playbook exists.

## Staff Engineer Lens

- Deployment architecture is part of software architecture.
- Rollback is easy for code and hard for data; design accordingly.
- Local production-like environments improve judgment before cloud complexity.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Java 25 container ergonomics** — Verify heap sizing, CPU count, GC selection, JFR access, and container memory behavior under Compose limits.
- **AOT/AppCDS experiment** — Evaluate Java 25 AOT command-line ergonomics and method profiling for startup experiments without making them mandatory.
- **Backup and restore drill** — Practice local PostgreSQL backup/restore and schema rollback documentation.
- **Configuration drift detector** — Compare local, CI, and prod-like configuration keys so missing or unused properties are visible.
- **Graceful shutdown acceptance test** — Stop containers during HTTP requests, Kafka consumption, and scheduled jobs to prove shutdown behavior.

---
