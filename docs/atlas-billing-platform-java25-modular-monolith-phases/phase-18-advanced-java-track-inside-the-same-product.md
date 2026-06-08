# Phase 18 — Advanced Java Track Inside the Same Product

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 18 of 18

## Business Goal

The platform is now complex enough that advanced Java features solve real problems instead of appearing as isolated exercises.

## Technical Goal

Use modern Java and JVM tooling in context.

## Concepts Introduced Naturally

| Java / JVM Concept | Where It Appears |
|---|---|
| Records | DTOs, commands, events, IDs |
| Sealed classes | payment states, invoice states, workflow states |
| Pattern matching | event handling and state transitions |
| Streams | reporting and projections |
| Virtual threads | blocking IO workloads in payment-service |
| Structured concurrency | parallel fraud check and provider lookup |
| CompletableFuture | comparison with structured concurrency |
| Reflection | custom validation and annotation scanning |
| Annotation processing | mapper or metadata generation |
| ThreadLocal | MDC and correlation ID propagation |
| ScopedValue | safer context propagation with virtual threads |
| Class loading | plugin-like payment provider adapters |
| GC tuning | load testing payment-service |
| JFR | production-like performance investigation |
| JMC | interpreting JFR recordings |
| JMH | benchmarking money calculations and mappers |
| Memory analysis | understanding heap growth under load |
| Thread analysis | diagnosing blocking, contention, and virtual-thread behavior |

## Practical Implementation Tasks

- Convert command and response models to records.
- Model payment workflow states with sealed interfaces/classes.
- Use pattern matching for event handlers.
- Add virtual-thread executor for blocking provider calls.
- Compare platform threads vs virtual threads under load.
- Use structured concurrency for:

```text
fraud check + provider capability lookup + customer risk profile lookup
```

- Compare structured concurrency to `CompletableFuture`.
- Use reflection for custom annotation scanning in validation module.
- Add annotation processor or compile-time metadata generator for domain events.
- Replace unsafe ThreadLocal assumptions with explicit context passing or ScopedValue where appropriate.
- Add JMH benchmarks for:
  - Money arithmetic
  - MapStruct mapper vs manual mapper
  - JSON serialization
- Capture JFR during payment-service load test.
- Analyze GC behavior with JMC.

## Architectural Decision

### Decision

Introduce advanced Java only where the platform creates a real need.

### Why

Senior and Staff Engineers should know not only how a feature works, but when it improves the system.

### Alternatives Considered

| Alternative | Why Not Enough |
|---|---|
| Separate Java exercises | Does not teach architectural trade-offs |
| Use every new Java feature everywhere | Creates novelty-driven code |
| Avoid advanced Java | Misses important modern JVM capabilities |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Applied language mastery | Requires deeper JVM understanding |
| Better performance diagnosis | More tooling to learn |
| Cleaner state modeling | Team needs Java feature familiarity |

## Testing and Acceptance Gate

| Test Type | What It Proves |
|---|---|
| Load tests | Virtual threads improve blocking workloads safely |
| JMH benchmarks | Micro-optimizations are measured correctly |
| JFR analysis | JVM bottlenecks are understood |
| Context propagation tests | Correlation works with virtual threads |
| Compile-time checks | Annotation processing generates expected metadata |

Acceptance criteria:

- Virtual-thread payment provider calls are measured against platform-thread baseline.
- JFR recording is captured and analyzed.
- Workflow states are modeled safely.
- Context propagation remains correct.

## Staff Engineer Lens

- Advanced Java is valuable when it reduces risk, improves clarity, or solves measured performance issues.
- JVM tooling matters more than memorizing flags.
- Modern concurrency must be paired with context propagation and observability.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Java 25 feature map** — Explicitly map Java 25 features used in the product: Scoped Values, module import declarations, flexible constructor bodies, compact source files for scripts, JFR improvements, KDF API, and preview/incubator features only where justified.
- **Structured concurrency preview guardrail** — Because structured concurrency is still preview in Java 25, keep it isolated and require `--enable-preview` only in modules that deliberately use it.
- **Stable Values preview experiment** — Evaluate Stable Values for expensive lazily initialized infrastructure objects, but keep it out of core billing logic until it is final.
- **Compact Object Headers experiment** — Benchmark memory-heavy read models or event buffers with and without compact object headers before adopting.
- **JFR method timing and tracing lab** — Use Java 25 JFR method timing/tracing to investigate a real bottleneck found during load tests.

---
