# Phase 9 — Caching, Read Models, and Performance

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 09 of 18

## Business Goal

Plan catalog, tax rates, entitlement checks, invoice lists, and reporting screens are read frequently.

## Technical Goal

Improve performance while making consistency trade-offs explicit.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Cache-aside | Common safe caching strategy for reads |
| Write-through | Some cached data needs immediate consistency |
| Write-behind | Useful but risky for financial data |
| Caffeine | Fast local cache for reference data |
| Redis | Shared cache for multi-instance state |
| TTL | Cached data needs expiry |
| Cache invalidation | Writes must make stale reads predictable |
| Hot keys | Popular plans or tenants can overload cache entries |
| Thundering herd | Expired popular keys can overload the database |
| Stale data | Some reads can tolerate delay; financial commands cannot |
| Local vs distributed cache | Speed and consistency differ |
| Materialized view | Expensive summaries can be precomputed |
| Read model projection | Query shape can differ from write model |
| CQRS read model | Reads and writes can evolve separately when justified |
| Database archival | Old invoices and events eventually need storage strategy |
| Data retention policy | Business and compliance decide retention |
| Soft delete vs hard delete | Deletion has audit and privacy trade-offs |
| Performance regression testing | Optimizations should not silently degrade |
| JFR and JMC | JVM-level performance needs real profiling |
| VisualVM | Early local CPU, heap, and thread inspection |
| JMeter/Gatling | API load needs repeatable tests |

## Practical Implementation Tasks

- Cache plan catalog using Caffeine.
- Cache tax rates using Redis.
- Do not cache mutable financial balances for command decisions.
- Add cache metrics:
  - hit rate
  - miss rate
  - eviction count
  - load time
- Add invalidation event when a plan changes.
- Simulate cache stampede and add protection:
  - per-key lock
  - early refresh
  - request coalescing
- Add invoice read model projection:

```text
invoice_summary_view
  invoice_id
  customer_id
  subscription_id
  amount
  currency
  status
  issued_at
  paid_at
```

- Compare JPA entity loading vs DTO projection.
- Add materialized view for monthly revenue report.
- Add refresh strategy for reporting view.
- Add archival plan for old outbox/inbox/idempotency records.
- Add performance baseline using JMeter or Gatling.
- Capture JFR recording during load test.

## Architectural Decision

### Decision

Use Caffeine for local reference data, Redis for shared cross-instance state, and projections for expensive reads.

### Why

Not all data has the same consistency needs. Billing commands need correctness; reporting screens can often tolerate delay.

### Alternatives Considered

| Alternative | Why Not Enough |
|---|---|
| Cache everything in Redis | Adds network hop and hides consistency risk |
| Cache everything locally | Inconsistent across instances |
| No cache | Database pressure grows unnecessarily |
| CQRS everywhere | Too much complexity before reads justify it |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Lower DB pressure | Invalidation complexity |
| Faster reads | Possible stale data |
| Clear read models | More projection code |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Cache consistency tests | Updates invalidate or refresh reads correctly |
| Stampede tests | Popular expired keys do not overload DB |
| Projection tests | Read models reflect source data |
| Load tests | Baseline performance is measurable |
| JFR analysis | JVM bottlenecks are visible |

Acceptance criteria:

- Plan catalog reads hit local cache.
- Tax rate reads use Redis safely.
- Financial commands do not rely on stale cache data.
- Monthly revenue report uses a projection or materialized view.

## Staff Engineer Lens

- Caching is a consistency decision disguised as a performance feature.
- Read models are justified when query needs diverge from write models.
- Performance work without baselines is guessing.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Cache consistency matrix** — For every cached value, document owner, TTL, invalidation trigger, stale-read tolerance, and whether it can be used for commands.
- **Cache key naming convention** — Include tenant, resource type, version, and relevant filters to avoid collisions and accidental cross-tenant data exposure.
- **Redis eviction and memory policy** — Configure and test eviction behavior so local behavior does not hide production risks.
- **Performance budget per endpoint** — Set initial p95/p99 latency targets for catalog reads, invoice lists, and payment commands.
- **Projection rebuild strategy** — Define how read models are rebuilt from source tables or events without corrupting user-visible reports.

---
