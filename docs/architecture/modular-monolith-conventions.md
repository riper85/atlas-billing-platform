# Modular Monolith Repository Conventions

## Starting rule

There is one deployable application: `apps/billing-app`.

Modules are used for source-code boundaries, not deployment boundaries. A module may become an extraction candidate only after a later phase proves operational and business reasons for extraction.

## Module types

| Area | Purpose |
|---|---|
| `apps/` | Deployable applications |
| `shared/` | Small stable primitives with low volatility |
| `platform/` | Build governance, BOMs, starters, and platform conventions |
| `services/` | Reserved for future extracted services, not used in Phase 0 |
| `infrastructure/` | Local infrastructure definitions |
| `observability/` | Dashboards, alerts, tracing config later |
| `docs/` | ADRs, runbooks, architecture notes |

## Boundary rules

- App modules may depend on platform and shared modules.
- Shared modules must not depend on app modules.
- Platform starters must not contain business logic.
- Business modules added later should expose explicit APIs and keep adapters/internal implementation private.
- No module should depend on another module just to reuse incidental implementation details.

## Extraction rule

Do not create a microservice because code is large. Consider extraction only when there is a clear reason such as independent scaling, separate data ownership, separate release cadence, or team ownership boundaries.
