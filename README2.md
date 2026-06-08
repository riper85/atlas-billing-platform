# Atlas Billing Platform

Java 25 modular-monolith learning platform.

This repository is Phase 0: **Engineering Workspace Foundation**. It establishes the build, dependency governance, quality gates, security scanning hooks, local commands, and documentation conventions before business code grows.

## What is implemented in Phase 0

- Maven multi-module reactor
- `platform-parent` for centralized build and plugin governance
- `platform-bom` for dependency version alignment
- Java 25 compiler release and toolchain checks
- Maven Enforcer rules for Java/Maven versions, convergence, banned dependencies, duplicate declarations, duplicate classes, and dynamic versions
- Spotless formatting
- Checkstyle, PMD, SpotBugs
- JaCoCo coverage reporting
- OWASP Dependency Check profile
- CycloneDX SBOM generation
- Gitleaks and Trivy configuration
- Makefile command surface
- ADR template and first architectural decision record
- Modular-monolith repository conventions
- Minimal Spring Boot billing app smoke test

## Prerequisites

Install these locally:

- Java 25
- Maven 3.9.x or newer
- Docker Desktop, optional for later phases
- `gitleaks`, optional but required by `make gitleaks`
- `trivy`, optional but required by `make trivy`

The repository intentionally targets Java 25 only. Java 21 compatibility is not kept.

## Maven Toolchains

Create `~/.m2/toolchains.xml` from `devops/maven-toolchains.xml.example` and update the JDK path for your machine.

```bash
mkdir -p ~/.m2
cp devops/maven-toolchains.xml.example ~/.m2/toolchains.xml
# edit ~/.m2/toolchains.xml and point jdkHome to your Java 25 installation
```

On macOS you can locate JDKs with:

```bash
/usr/libexec/java_home -V
```

## Main commands

```bash
make doctor
make build
make test
make verify
make format
make gitleaks
make trivy
make sbom
make clean
```

`make verify` runs Maven verification with the `quality` and `security` profiles. The first OWASP Dependency Check execution can be slow because it downloads vulnerability data.

## Application smoke run

After a successful build:

```bash
java -jar apps/billing-app/target/billing-app-0.1.0-SNAPSHOT.jar
```

Then call:

```bash
curl http://localhost:8080/internal/readiness
```

Expected response:

```json
{"app":"atlas-billing-platform","phase":"phase-0","status":"ok"}
```

## Repository layout

```text
atlas-billing-platform/
  platform/
    platform-bom/
    platform-parent/
    platform-starters/
      platform-starter-web/
  shared/
    shared-kernel/
  apps/
    billing-app/
  services/
  infrastructure/
    docker/
  observability/
  devops/
  docs/
    adr/
    architecture/
    engineering/
    runbooks/
  scripts/
  sandbox/
```

## Phase 0 acceptance gate

```bash
make verify
make gitleaks
make sbom
```

If those pass locally with Java 25, Phase 0 is complete.
