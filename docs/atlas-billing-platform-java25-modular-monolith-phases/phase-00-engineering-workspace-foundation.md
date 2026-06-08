# Phase 0 — Engineering Workspace Foundation

> **Target runtime:** Java 25  
> **Architecture stance:** Start as a modular monolith. Keep one deployable application unless a later phase explicitly validates an extraction candidate.  
> **Phase file:** 00 of 18

## Business Goal

Create a consistent local development environment so any developer can clone the repository, run the platform, and trust the build.

## Technical Goal

Establish project structure, build governance, dependency control, code quality checks, and local automation before business code grows.

## Why This Phase Exists

Most architecture problems start quietly: inconsistent dependencies, slow builds, broken local environments, missing quality gates, or undocumented setup steps.

At Staff Engineer level, the build is part of the architecture.

## Concepts Introduced Naturally

| Concept | Why It Appears Now |
|---|---|
| Maven reactor | Multiple modules need deterministic build ordering |
| Parent POM | Common plugin and build configuration must be centralized |
| BOM | Dependency versions must not drift across modules |
| Maven Enforcer | Java 25 version and dependency convergence must be enforced |
| Plugin management | Build behavior should be predictable |
| Reproducible builds | Same source should create same artifact |
| Spotless / Checkstyle / PMD / SpotBugs | Code quality should not depend only on review comments |
| JaCoCo | Test coverage visibility starts from day one |
| OWASP Dependency Check | Dependency risk must be visible early |
| Gitleaks | Secrets must not enter Git history |
| Trivy | Container images will eventually need scanning |
| SBOM | Supply-chain visibility starts before release pressure |
| Makefile | Common local commands reduce onboarding friction |
| ADR template | Decisions should be captured while context is fresh |



## Java 25 Baseline

- Set Maven compiler `release` to `25`.
- Use Maven Toolchains so the build fails if Java 25 is not available.
- Add `.sdkmanrc` or `.java-version` for local developer consistency.
- Decide whether preview features are allowed. If allowed, isolate them and require explicit `--enable-preview` in compiler, test, and runtime configuration.
- Treat Java 25 as the only target; do not keep Java 21 compatibility unless a phase explicitly says to compare behavior.

## Target Repository Structure

```text
atlas-billing-platform/
  platform/
    platform-bom/
    platform-parent/
    platform-starters/
  shared/
  apps/
    billing-app/
  services/
  infrastructure/
    docker/
  observability/
  devops/
  docs/
    adr/
    runbooks/
  scripts/
  sandbox/
```

## Practical Implementation Tasks

- Create the root repository.
- Create a Maven multi-module structure.
- Create `platform-parent` for common build configuration.
- Create `platform-bom` for dependency version alignment.
- Add Maven Enforcer rules:
  - Java 25 version
  - dependency convergence
  - banned dependencies
  - duplicate classes
- Add Spotless formatting.
- Add Checkstyle rules.
- Add SpotBugs and PMD.
- Add JaCoCo with low initial thresholds.
- Add OWASP Dependency Check.
- Add Gitleaks.
- Add Trivy configuration for future container images.
- Add SBOM generation using CycloneDX Maven plugin.
- Add Makefile commands:

```bash
make build
make test
make verify
make up
make down
make logs
make clean
```

## Architectural Decision

### Decision

Use a Maven multi-module monorepo.

### Why

At the beginning, refactoring speed matters more than independent deployment. Boundaries are still being discovered.

### Alternatives Considered

| Alternative | Why Not Yet |
|---|---|
| Many repositories | Too much coordination before service boundaries are proven |
| Gradle | Good option, but Maven is common in enterprise Java and simpler for this path |
| Single flat project | Becomes hard to govern once modules grow |

### Trade-Offs

| Benefit | Cost |
|---|---|
| Fast refactoring | Repository can become large |
| Centralized dependency governance | Requires discipline |
| Easier local onboarding | Initial setup takes time |

## Testing and Acceptance Gate

| Check | Acceptance Criteria |
|---|---|
| Build | `mvn clean verify` succeeds |
| Formatting | Spotless check passes |
| Dependency health | Enforcer and dependency check pass |
| Security hygiene | Gitleaks finds no secrets |
| Documentation | ADR template exists |

## Staff Engineer Lens

- The build system is a control plane for engineering quality.
- Architecture rules that are not automated become suggestions.
- Dependency drift is cheaper to prevent than to repair.

## Missing Concepts Added at the End of This Phase

These additions keep the original phase intact and extend it with concepts that are useful for a Java 25 modular-monolith implementation.

- **Java 25 toolchain governance** — Pin Java 25 with Maven Toolchains, `.sdkmanrc` or `jenv`, Maven Compiler Plugin `release=25`, and CI checks so every developer builds with the same JDK.
- **Preview/incubator feature policy** — Document when `--enable-preview` is allowed, which modules may use it, and how preview code is isolated from core business paths.
- **Spring Boot + Java 25 compatibility gate** — Add a CI smoke test that proves the chosen Spring Boot, Maven Surefire/Failsafe, SpotBugs, JaCoCo, and Testcontainers versions work on Java 25.
- **Modular-monolith repository convention** — Define from day one where module APIs, internal implementations, adapters, and tests live.
- **Dependency update automation** — Add Renovate or Dependabot rules, but require human review for Spring Boot, Hibernate, Kafka, database driver, and security-sensitive upgrades.

---
