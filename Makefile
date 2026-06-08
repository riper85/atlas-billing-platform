SHELL := /bin/bash
MVN ?= mvn
COMPOSE ?= docker compose
COMPOSE_FILE ?= infrastructure/docker/compose.yaml

.PHONY: doctor build test verify verify-full format check security sbom gitleaks trivy up down logs clean

doctor:
	@echo "Java:"
	@java -version
	@echo
	@echo "Maven:"
	@$(MVN) -version
	@echo
	@echo "Optional tools:"
	@command -v gitleaks >/dev/null && gitleaks version || echo "gitleaks not installed"
	@command -v trivy >/dev/null && trivy --version | head -1 || echo "trivy not installed"

build:
	$(MVN) -DskipTests package

test:
	$(MVN) test

verify:
	$(MVN) clean verify -Pquality

verify-full:
	$(MVN) clean verify -Pquality,security

FORMAT_MODULES := :platform-parent,:platform-starter-web,:platform-starters,:shared-kernel,:billing-app

format:
	$(MVN) -pl $(FORMAT_MODULES) -Pquality com.diffplug.spotless:spotless-maven-plugin:3.1.0:apply

check:
	$(MVN) verify -Pquality -DskipTests

security:
	$(MVN) verify -Psecurity

sbom:
	$(MVN) -Psecurity cyclonedx:makeAggregateBom

gitleaks:
	@command -v gitleaks >/dev/null || (echo "Install gitleaks first: https://github.com/gitleaks/gitleaks" && exit 1)
	gitleaks detect --config .gitleaks.toml --source . --verbose

trivy:
	@command -v trivy >/dev/null || (echo "Install trivy first: https://github.com/aquasecurity/trivy" && exit 1)
	trivy fs --config .trivy.yaml .

up:
	@echo "Phase 0 has no long-running infrastructure yet. Compose file is present for later phases."
	$(COMPOSE) -f $(COMPOSE_FILE) config >/dev/null

logs:
	@echo "Phase 0 has no long-running infrastructure logs yet."

down:
	$(COMPOSE) -f $(COMPOSE_FILE) down --remove-orphans

clean:
	$(MVN) clean
	@rm -rf .scannerwork dependency-check-report.* bom.json bom.xml